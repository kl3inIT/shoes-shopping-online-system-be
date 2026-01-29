# syntax=docker/dockerfile:1.7

# ============================================================================
# STAGE 1: Application builder with optimized caching
# ============================================================================
FROM eclipse-temurin:21-jdk-alpine@sha256:89517925fa675c6c4b770bee7c44d38a7763212741b0d6fca5a5103caab21a97 AS builder

ENV GRADLE_USER_HOME=/cache/.gradle \
    LANG=C.UTF-8 \
    TZ=UTC

# Install build dependencies (minimal)
RUN apk add --no-cache binutils && \
  rm -rf /var/cache/apk/*

WORKDIR /build

# Copy Gradle wrapper and dependency definition files first
# This layer will be cached until these files change
COPY gradle/ gradle/
COPY gradlew build.gradle ./

# Download dependencies with BuildKit cache mount for faster subsequent builds
RUN --mount=type=cache,id=gradle-cache,target=/cache/.gradle,sharing=locked \
  chmod +x gradlew && \
  ./gradlew dependencies --no-daemon --parallel --console=plain

# Copy only production source code (exclude tests, docs, etc.)
COPY src/main/ src/main/

# Build optimized JAR with cache mount
RUN --mount=type=cache,id=gradle-cache,target=/cache/.gradle,sharing=locked \
  ./gradlew bootJar --no-daemon --parallel --console=plain -x test && \
  mkdir -p /app && \
  mv build/libs/*.jar /app/app.jar

WORKDIR /app

# Analyze the dependencies contained into the fat jar
RUN jdeps --ignore-missing-deps -q \
  --recursive \
  --multi-release 21 \
  --print-module-deps \
  --class-path 'BOOT-INF/lib/*' \
  app.jar > deps.info

# Create the custom JRE
RUN jlink \
  --verbose \
  --add-modules $(cat deps.info),java.desktop,java.management,java.logging,java.naming,java.security.jgss,java.instrument,java.sql,jdk.unsupported,java.compiler \
  --compress zip-9 \
  --no-header-files \
  --no-man-pages \
  --output /jre-minimal

# Extract Spring Boot layers for optimal Docker layer caching
RUN java -Djarmode=layertools -jar app.jar extract --destination /app/extracted

# ============================================================================
# STAGE 3: Minimal runtime image
# ============================================================================
FROM alpine:3.21@sha256:5405e8f36ce1878720f71217d664aa3dea32e5e5df11acbf07fc78ef5661465b

# Install only critical runtime dependencies
# ca-certificates: for HTTPS connections
# tini: proper init system for PID 1
# tzdata: timezone support
# curl: for healthcheck

RUN apk add --no-cache \
  ca-certificates \
  tini \
  curl && \
  rm -rf /var/cache/apk/* /tmp/*

# Create non-root user for security (CIS Docker Benchmark compliance)
RUN addgroup -g 1654 -S appgroup && \
  adduser -u 1654 -S appuser -G appgroup

# Copy minimal custom JRE from builder
COPY --from=builder --chown=1654:1654 /jre-minimal /opt/java

# Set up application directory with proper ownership
WORKDIR /app

# Copy Spring Boot layers in optimal order (least to most frequently changed)
# This maximizes Docker layer cache efficiency
COPY --from=builder --chown=1654:1654 /app/extracted/dependencies/ ./
COPY --from=builder --chown=1654:1654 /app/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=1654:1654 /app/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=1654:1654 /app/extracted/application/ ./

# Switch to non-root user (security best practice)
USER 1654:1654

# Set JAVA_HOME and PATH
ENV JAVA_HOME=/opt/java \
  PATH="/opt/java/bin:${PATH}"

# Optimal JVM flags for containerized Spring Boot applications
# - UseContainerSupport: respect container memory limits
# - MaxRAMPercentage: use max 75% of container memory for heap
# - UseG1GC: best GC for containers with predictable pause times
# - UseStringDeduplication: reduce memory footprint
# - ExitOnOutOfMemoryError: fail fast on OOM
# - TieredCompilation with level 1: faster startup, good for short-lived containers
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport \
  -XX:MaxRAMPercentage=75.0 \
  -XX:InitialRAMPercentage=50.0 \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=100 \
  -XX:+UseStringDeduplication \
  -XX:+ParallelRefProcEnabled \
  -XX:+DisableExplicitGC \
  -XX:+ExitOnOutOfMemoryError \
  -Djava.security.egd=file:/dev/./urandom \
  -Djava.awt.headless=true"

# Application server port
EXPOSE 8088

# Comprehensive OCI labels for traceability and compliance
LABEL org.opencontainers.image.title="Dockerfile của PHD" \
  org.opencontainers.image.description="Spring Boot application containerized with custom JRE and layered build" \
  org.opencontainers.image.vendor="PHD" \
  org.opencontainers.image.authors="PHD dathip04@gmail.com" \
  org.opencontainers.image.source="https://github.com/kl3inIT/shoes-shopping-online-system-be" \
  org.opencontainers.image.version="1.0.0" \
  org.opencontainers.image.revision="main" \
  org.opencontainers.image.licenses="MIT" \
  org.opencontainers.image.base.name="docker.io/library/alpine:3.21" \
  org.opencontainers.image.base.digest="sha256:5405e8f36ce1878720f71217d664aa3dea32e5e5df11acbf07fc78ef5661465b" \
  maintainer="PHD"


# Health check using Spring Boot Actuator /health endpoint
# Using curl for lightweight health checks
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8088/actuator/health || exit 1

# Use tini as init system for proper signal handling
# Ensures graceful shutdown and zombie process reaping
ENTRYPOINT ["/sbin/tini", "--"]

# Run Spring Boot application
# Using exec form to ensure proper signal propagation
CMD ["java", "org.springframework.boot.loader.launch.JarLauncher"]