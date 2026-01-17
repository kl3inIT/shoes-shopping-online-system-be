# Tài Liệu Tích Hợp KeyCloak với Spring Boot 3

## Mục Lục
1. [Tổng Quan](#tổng-quan)
2. [Kiến Trúc Tổng Thể](#kiến-trúc-tổng-thể)
3. [Luồng Xác Thực](#luồng-xác-thực)
4. [Các Thành Phần Chính](#các-thành-phần-chính)
5. [Cấu Hình Chi Tiết](#cấu-hình-chi-tiết)
6. [Logic Xử Lý JWT](#logic-xử-lý-jwt)
7. [Phân Quyền và Bảo Mật](#phân-quyền-và-bảo-mật)
8. [Testing](#testing)
9. [Troubleshooting](#troubleshooting)

---

## Tổng Quan

Dự án này tích hợp **KeyCloak** như một **Authorization Server** (OAuth2/OIDC) với **Spring Boot 3** để thực hiện:
- Xác thực người dùng (Authentication) thông qua JWT tokens
- Phân quyền (Authorization) dựa trên roles
- Stateless authentication (không lưu session trên server)

### Công Nghệ Sử Dụng
- **Spring Boot 4** (Java 21)
- **Spring Security** với OAuth2 Resource Server
- **KeyCloak** (Identity and Access Management)
- **JWT** (JSON Web Tokens)
- **Docker** (để chạy KeyCloak)

---

## Kiến Trúc Tổng Thể

```
┌─────────────┐         ┌──────────────┐         ┌─────────────┐
│   Client    │────────▶│ Spring Boot  │────────▶│  KeyCloak   │
│  (Postman/  │  JWT    │  Application │ Validate│  (Auth      │
│  Frontend)  │  Token  │              │  Token  │   Server)   │
└─────────────┘         └──────────────┘         └─────────────┘
                              │
                              │ Extract Roles
                              │ & User Info
                              ▼
                        ┌──────────────┐
                        │ Authorization│
                        │   Decision   │
                        └──────────────┘
```

### Luồng Hoạt Động:
1. **Client** gửi request kèm JWT token trong header `Authorization: Bearer {token}`
2. **Spring Boot** nhận request và validate JWT token với KeyCloak
3. **JwtConverter** extract thông tin user và roles từ JWT
4. **SecurityConfig** kiểm tra quyền truy cập dựa trên roles
5. Cho phép hoặc từ chối request

---

## Luồng Xác Thực

### Bước 1: Lấy Access Token từ KeyCloak

Client cần lấy token trước khi gọi API:

```bash
curl --location 'http://localhost:8080/realms/ssos/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=ssos-backend' \
--data-urlencode 'username=admin' \
--data-urlencode 'password=123456'
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "expires_in": 300,
  "refresh_token": "...",
  "token_type": "Bearer"
}
```

### Bước 2: Sử Dụng Token trong API Request

```bash
curl --location 'http://localhost:8088/test' \
--header 'Authorization: Bearer eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...'
```

### Bước 3: Spring Boot Xử Lý Request

1. **Spring Security Filter Chain** intercept request
2. **OAuth2ResourceServer** extract JWT token từ header
3. **JwtDecoder** validate token với KeyCloak (kiểm tra signature, expiration, issuer)
4. **JwtConverter** convert JWT thành `Authentication` object
5. **SecurityConfig** kiểm tra authorization rules
6. Cho phép hoặc từ chối request

---

## Các Thành Phần Chính

### 1. ApplicationProperties

**File:** `src/main/java/com/sba/ssos/configuration/ApplicationProperties.java`

```java
@ConfigurationProperties(prefix = "application-properties")
public record ApplicationProperties(
    String clientName,           // Tên client trong KeyCloak
    List<String> adminPrivilegeUrls,  // URLs yêu cầu ROLE_ADMIN
    List<String> noAuthUrls      // URLs không cần authentication
)
```

**Mục đích:** Lưu trữ các cấu hình security từ `application.yml`

**Logic:**
- Sử dụng Java Record (Spring Boot 3 feature)
- Tự động bind từ `application.yml` với prefix `application-properties`
- Immutable và type-safe

### 2. SecurityConfig

**File:** `src/main/java/com/sba/ssos/security/SecurityConfig.java`

**Chức năng chính:**

#### a. SecurityFilterChain Configuration

```47:60:src/main/java/com/sba/ssos/security/SecurityConfig.java
        .authorizeHttpRequests(
            customizer ->
                customizer
                    .requestMatchers(asArray(applicationProperties.noAuthUrls()))
                    .permitAll()
                    .requestMatchers(asArray(applicationProperties.adminPrivilegeUrls()))
                    .hasAuthority(UserRole.ROLE_ADMIN.name())
                    .anyRequest()
                    .authenticated())
        .oauth2ResourceServer(
            customizer ->
                customizer.jwt(
                    jwtConfigurer -> jwtConfigurer.jwtAuthenticationConverter(jwtConverter)))
```

**Logic phân quyền:**
1. **permitAll()**: Cho phép truy cập các URL trong `noAuthUrls` (Swagger, Actuator, etc.)
2. **hasAuthority(ROLE_ADMIN)**: Yêu cầu `ROLE_ADMIN` cho các URL trong `adminPrivilegeUrls`
3. **authenticated()**: Tất cả request khác cần authentication (bất kỳ role nào)
4. **oauth2ResourceServer**: Cấu hình OAuth2 Resource Server với custom JWT converter

#### b. Role Hierarchy

```63:70:src/main/java/com/sba/ssos/security/SecurityConfig.java
  @Bean
  public RoleHierarchy roleHierarchy() {
    var roleHierarchy = "%s > %s".formatted(UserRole.ROLE_ADMIN, UserRole.ROLE_USER);

    log.info("Role hierarchy configured -- {}", roleHierarchy);

    return RoleHierarchyImpl.fromHierarchy(roleHierarchy);
  }
```

**Logic:**
- `ROLE_ADMIN > ROLE_USER`: Admin có tất cả quyền của User
- Nếu endpoint yêu cầu `ROLE_USER`, user có `ROLE_ADMIN` vẫn có thể truy cập

#### c. CORS Configuration

```72:88:src/main/java/com/sba/ssos/security/SecurityConfig.java
  private static CorsConfigurationSource corsConfigurationSource() {
    var corsConfigurationSource = new UrlBasedCorsConfigurationSource();

    var corsConfiguration = new CorsConfiguration();

    corsConfiguration.setAllowCredentials(true);

    var everything = List.of("*");

    corsConfiguration.setAllowedOriginPatterns(everything);
    corsConfiguration.setAllowedHeaders(everything);
    corsConfiguration.setAllowedMethods(everything);

    corsConfigurationSource.registerCorsConfiguration("/**", corsConfiguration);

    return corsConfigurationSource;
  }
```

**Lưu ý:** Cấu hình này cho phép tất cả origins (development only). Production nên giới hạn cụ thể.

### 3. JwtConverter

**File:** `src/main/java/com/sba/ssos/security/JwtConverter.java`

**Đây là thành phần QUAN TRỌNG NHẤT** - chuyển đổi JWT token thành Spring Security Authentication object.

#### Logic Chi Tiết:

##### Bước 1: Validate Authorized Party (azp)

```33:37:src/main/java/com/sba/ssos/security/JwtConverter.java
        // cannot have different authorized party
        if (!clientName.equalsIgnoreCase(jwt.getClaimAsString("azp"))) {
            throw new AuthorizationException(
                    "Invalid authorized party (azp), expected [%s]".formatted(clientName));
        }
```

**Mục đích:** Đảm bảo token được phát hành cho đúng client (`ssos-backend`)

##### Bước 2: Extract Resource Access Claim

```39:46:src/main/java/com/sba/ssos/security/JwtConverter.java
        // get the top-level "resource_access" claim.
        var resourceAccess =
                nonMissing(jwt.getClaimAsMap(RESOURCE_ACCESS_CLAIM), RESOURCE_ACCESS_CLAIM);

        // get the map specific to our client ID.
        var clientRolesMap =
                (Map<String, Collection<String>>)
                        getMapValue(resourceAccess, clientName, RESOURCE_ACCESS_CLAIM);
```

**Cấu trúc JWT từ KeyCloak:**
```json
{
  "resource_access": {
    "ssos-backend": {
      "roles": ["role_admin", "role_user"]
    }
  },
  "azp": "ssos-backend",
  "sub": "user-uuid",
  "preferred_username": "admin",
  "email": "admin@example.com"
}
```

**Logic:**
- Lấy `resource_access` claim (chứa roles của các clients)
- Lấy phần tử tương ứng với `clientName` (`ssos-backend`)
- Đây là nơi KeyCloak lưu client-specific roles

##### Bước 3: Extract Roles

```48:61:src/main/java/com/sba/ssos/security/JwtConverter.java
        // get the collection of role strings from that map.
        var roleNames = getMapValue(clientRolesMap, "roles", RESOURCE_ACCESS_CLAIM, clientName);

        var authorities =
                roleNames.stream()
                        // roughly equivalent to:
                        // .filter(StringUtils::nonNull).map(e -> new SimpleGrantedAuthority(e.toUpperCase))
                        .<GrantedAuthority>mapMulti(
                                (element, downstream) -> {
                                    if (StringUtils.isNotBlank(element)) {
                                        downstream.accept(new SimpleGrantedAuthority(element.toUpperCase()));
                                    }
                                })
                        .collect(Collectors.toSet());
```

**Logic:**
- Lấy mảng `roles` từ `clientRolesMap`
- Convert mỗi role thành `SimpleGrantedAuthority` (uppercase)
- Filter các role rỗng/null

**Ví dụ:** `["role_admin", "role_user"]` → `[ROLE_ADMIN, ROLE_USER]`

##### Bước 4: Tạo UserDetails Object

```63:72:src/main/java/com/sba/ssos/security/JwtConverter.java
        var userDetails =
                AuthorizedUserDetails.builder()
                        .userId(UUID.fromString(nonMissing(jwt.getSubject(), "subject")))
                        .username(nonMissing(jwt.getClaimAsString("preferred_username"), "username"))
                        .email(nonMissing(jwt.getClaimAsString(EMAIL_CLAIM), EMAIL_CLAIM))
                        .authorities(authorities)
                        .build();

        return UsernamePasswordAuthenticationToken.authenticated(
                userDetails, jwt.getTokenValue(), authorities);
```

**Logic:**
- Extract thông tin user từ JWT claims:
  - `sub`: User ID (UUID)
  - `preferred_username`: Username
  - `email`: Email
- Tạo `AuthorizedUserDetails` object
- Return `UsernamePasswordAuthenticationToken` với user details và authorities

**Tại sao cần `nonMissing()`?**
- Đảm bảo các claim bắt buộc phải có trong JWT
- Nếu thiếu → throw `AuthorizationException` → 401 Unauthorized

### 4. AuthorizedUserDetails

**File:** `src/main/java/com/sba/ssos/security/AuthorizedUserDetails.java`

```java
@Builder
public record AuthorizedUserDetails(
    UUID userId, 
    String username, 
    String email, 
    Collection<GrantedAuthority> authorities
) implements UserDetails
```

**Mục đích:**
- Custom implementation của `UserDetails`
- Lưu trữ thông tin user từ JWT
- Có thể access trong Controller qua `@AuthenticationPrincipal`

**Đặc điểm:**
- `getPassword()` return `null` (JWT không chứa password)
- `getAuthorities()` return roles từ JWT

---

## Cấu Hình Chi Tiết

### application.yml

```1:24:src/main/resources/application.yml
application-properties:
  realm-name: ssos
  client-name: ssos-backend
  admin-privilege-urls:
    - /test/admin/**
  no-auth-urls:
    # OpenAPI Swagger URLs
    - /swagger-ui.html
    - /swagger-ui/**
    - /v3/api-docs/**
    - /v3/api-docs.yaml
    # Actuator endpoints:
    - /actuator/**
    # Custom no-auth URLs:
    - /test/free
server.port: 8088
spring:
  datasource:
    url: jdbc:postgresql://${POSTGRESQL_HOST:localhost}:${POSTGRESQL_PORT:5432}/${POSTGRESQL_NAME}
    username: ${POSTGRESQL_USERNAME:postgres}
    password: ${POSTGRESQL_PASSWORD:123456}
#  threads.virtual.enabled: true # Make use of Spring Boot 3.2+ Virtual Threads support
  security.oauth2.resourceserver:
    jwt.issuer-uri: http://${KEYCLOAK_HOST:localhost:8080}/realms/${application-properties.realm-name}
```

**Giải thích:**

1. **application-properties:**
   - `realm-name`: Tên realm trong KeyCloak (`ssos`)
   - `client-name`: Tên client trong KeyCloak (`ssos-backend`)
   - `admin-privilege-urls`: URLs yêu cầu `ROLE_ADMIN`
   - `no-auth-urls`: URLs không cần authentication

2. **spring.security.oauth2.resourceserver.jwt.issuer-uri:**
   - URL của KeyCloak realm
   - Spring tự động fetch public key từ `{issuer-uri}/.well-known/openid-configuration`
   - Sử dụng public key để validate JWT signature

### Docker Compose (KeyCloak)

```1:34:docker/keycloak/docker-compose.yml
services:
  keycloak:
    image: quay.io/keycloak/keycloak:${KEYCLOAK_VERSION:-latest}
    container_name: keycloak
    ports:
      - "8080:8080"
    environment:
      KC_DB: postgres
      KC_DB_URL: jdbc:postgresql://${POSTGRES_HOST}/${POSTGRES_DB}
      KC_DB_USERNAME: ${POSTGRES_USER}
      KC_DB_PASSWORD: ${POSTGRES_PASSWORD}
      KEYCLOAK_ADMIN: ${KEYCLOAK_ADMIN}
      KEYCLOAK_ADMIN_PASSWORD: ${KEYCLOAK_ADMIN_PASSWORD}
      KC_HEALTH_ENABLED: true
      KC_METRICS_ENABLED: true
      JAVA_OPTS_KC_HEAP: >
        -XX:InitialRAMPercentage=35
        -XX:MaxRAMPercentage=60
        -XX:MinHeapFreeRatio=20
        -XX:MaxHeapFreeRatio=30

    mem_limit: 1200m
    command: start-dev
    volumes:
      - ./keycloak/themes:/opt/keycloak/themes
    restart: always
    healthcheck:
      test: [ "CMD-SHELL", "exec 3<>/dev/tcp/localhost/8080 && echo -e 'GET /health/ready HTTP/1.1\r\nHost: localhost\r\n\r\n' >&3 && grep '200 OK' <&3" ]
      interval: 10s
      timeout: 3s
      retries: 10
      start_period: 30s
```

**Lưu ý:** 
- `start-dev`: Development mode (không persistent, restart sẽ mất data)
- Production nên dùng `start` và cấu hình database

---

## Logic Xử Lý JWT

### JWT Structure từ KeyCloak

```json
{
  "iss": "http://localhost:8080/realms/ssos",
  "sub": "123e4567-e89b-12d3-a456-426614174000",
  "azp": "ssos-backend",
  "preferred_username": "admin",
  "email": "admin@example.com",
  "resource_access": {
    "ssos-backend": {
      "roles": ["role_admin", "role_user"]
    }
  },
  "exp": 1234567890,
  "iat": 1234567890
}
```

### Quá Trình Validate JWT

1. **Spring Security JwtDecoder:**
   - Fetch public key từ KeyCloak (`{issuer-uri}/.well-known/openid-configuration`)
   - Validate signature bằng public key
   - Validate `iss` (issuer) phải match với `issuer-uri`
   - Validate `exp` (expiration time)

2. **JwtConverter:**
   - Validate `azp` (authorized party)
   - Extract roles từ `resource_access.{client-name}.roles`
   - Extract user info từ claims
   - Tạo `Authentication` object

3. **SecurityConfig:**
   - Kiểm tra authorization rules
   - Cho phép hoặc từ chối request

---

## Phân Quyền và Bảo Mật

### Authorization Rules

| URL Pattern | Yêu Cầu | Ví Dụ |
|------------|---------|-------|
| `/swagger-ui/**` | Không cần auth | Public access |
| `/test/free` | Không cần auth | Public access |
| `/test/admin/**` | `ROLE_ADMIN` | Admin only |
| `/test/**` | Authenticated (bất kỳ role) | User hoặc Admin |
| Tất cả khác | Authenticated | User hoặc Admin |

### Role Hierarchy

```
ROLE_ADMIN
    ↓ (có tất cả quyền của)
ROLE_USER
```

**Ví dụ:**
- Endpoint yêu cầu `ROLE_USER` → `ROLE_ADMIN` có thể truy cập
- Endpoint yêu cầu `ROLE_ADMIN` → `ROLE_USER` KHÔNG thể truy cập

### Security Features

1. **Stateless Authentication:**
   - Không lưu session trên server
   - Mỗi request phải có JWT token
   - Token chứa đầy đủ thông tin user và roles

2. **XSS Protection:**
   ```java
   .xssProtection(xssConfig -> 
       xssConfig.headerValue(XXssProtectionHeaderWriter.HeaderValue.ENABLED_MODE_BLOCK))
   ```

3. **Content Security Policy:**
   ```java
   .contentSecurityPolicy(cps -> cps.policyDirectives("script-src 'self'"))
   ```

4. **CSRF Disabled:**
   - Vì sử dụng stateless JWT, không cần CSRF protection

---

## Testing

### 1. Test Public Endpoint (Không cần token)

```bash
curl http://localhost:8088/test/free
```

**Expected:** 200 OK

### 2. Test Protected Endpoint (Cần token)

**Bước 1:** Lấy token
```bash
curl --location 'http://localhost:8080/realms/ssos/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=ssos-backend' \
--data-urlencode 'username=user' \
--data-urlencode 'password=123456'
```

**Bước 2:** Sử dụng token
```bash
curl --location 'http://localhost:8088/test' \
--header 'Authorization: Bearer {access_token}'
```

**Expected:** 200 OK với user info

### 3. Test Admin Endpoint (Cần ROLE_ADMIN)

**Bước 1:** Lấy token với user có `role_admin`
```bash
curl --location 'http://localhost:8080/realms/ssos/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=ssos-backend' \
--data-urlencode 'username=admin' \
--data-urlencode 'password=123456'
```

**Bước 2:** Gọi admin endpoint
```bash
curl --location 'http://localhost:8088/test/admin' \
--header 'Authorization: Bearer {access_token}'
```

**Expected:** 200 OK

**Nếu dùng token của user chỉ có `role_user`:**
**Expected:** 403 Forbidden

### 4. Test Invalid Token

```bash
curl --location 'http://localhost:8088/test' \
--header 'Authorization: Bearer invalid_token'
```

**Expected:** 401 Unauthorized

---

## Troubleshooting

### 1. "Client not allowed for direct access grants" error

**Nguyên nhân:** Client trong KeyCloak chưa enable "Direct Access Grants"

**Giải pháp:**
1. Vào KeyCloak Admin Console
2. Chọn Realm → Clients → `ssos-backend`
3. Enable toggle "Direct Access Grants"
4. Save

### 2. "Account is not fully set up" error

**Nguyên nhân:** User trong KeyCloak chưa được set password hoặc chưa verify email

**Giải pháp:**
1. Vào KeyCloak Admin Console
2. Chọn Realm → Users → Chọn user
3. Set password trong tab "Credentials"
4. Disable "Temporary" nếu muốn password permanent

### 3. "Realm does not exist" error

**Nguyên nhân:** Realm name trong `application.yml` không khớp với KeyCloak

**Giải pháp:**
- Kiểm tra `application-properties.realm-name` trong `application.yml`
- Đảm bảo realm đã được tạo trong KeyCloak

### 4. "Invalid authorized party (azp)" error

**Nguyên nhân:** Token được phát hành cho client khác

**Giải pháp:**
- Kiểm tra `client_id` khi lấy token phải match với `application-properties.client-name`
- Đảm bảo token được lấy từ đúng client

### 5. "Claim [resource_access] is missing" error

**Nguyên nhân:** Token không có `resource_access` claim (thường do client chưa có roles)

**Giải pháp:**
1. Vào KeyCloak Admin Console
2. Chọn Realm → Clients → `ssos-backend` → Roles
3. Tạo roles: `role_admin`, `role_user`
4. Assign roles cho users trong tab "Users" → "Role Mappings"

### 6. Token expired

**Nguyên nhân:** JWT token đã hết hạn (thường 5 phút)

**Giải pháp:**
- Lấy token mới
- Hoặc sử dụng `refresh_token` để lấy access token mới

---

## Tóm Tắt Logic Tích Hợp

1. **KeyCloak** đóng vai trò **Authorization Server** - phát hành JWT tokens
2. **Spring Boot** đóng vai trò **Resource Server** - validate và sử dụng tokens
3. **JwtConverter** extract roles từ JWT payload (`resource_access.{client-name}.roles`)
4. **SecurityConfig** áp dụng authorization rules dựa trên roles
5. **Role Hierarchy** cho phép admin có tất cả quyền của user

### Điểm Quan Trọng:

- ✅ JWT token phải được validate với KeyCloak (signature, expiration, issuer)
- ✅ Roles được lấy từ `resource_access.{client-name}.roles` trong JWT
- ✅ Authorization rules được định nghĩa trong `SecurityConfig`
- ✅ Stateless authentication - không lưu session
- ✅ Role hierarchy: `ROLE_ADMIN > ROLE_USER`

---

## Tài Liệu Tham Khảo

- [Spring Security OAuth2 Resource Server](https://docs.spring.io/spring-security/reference/servlet/oauth2/resource-server/index.html)
- [KeyCloak Documentation](https://www.keycloak.org/documentation)
- [OAuth2 / OIDC Specification](https://oauth.net/2/)

---

**Tác giả:** kl3in
