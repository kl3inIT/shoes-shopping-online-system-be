## Shoes Shopping Online System – Backend

Backend cho hệ thống **Shoes Shopping Online System**, xây dựng bằng **Spring Boot 4 (Java 21)**, sử dụng **Spring Security OAuth2 Resource Server** để verify JWT từ **Keycloak**, **Spring Data JPA + PostgreSQL + Liquibase** cho dữ liệu, tích hợp **Keycloak Admin Client** để quản lý user, và một số module AI (Spring AI) phục vụ chat/search nội bộ.

### 1. Tech stack chính

- **Ngôn ngữ & runtime**
  - Java 21, Gradle
  - Spring Boot 4.0.x
- **Persistence**
  - Spring Data JPA
  - PostgreSQL
  - Liquibase (migration, `classpath:db/changelog/master.yaml`)
- **Security & auth**
  - Spring Security
  - OAuth2 Resource Server (JWT)
  - Keycloak (realm: `ssos-realm`, client: `ssos-app`)
  - Keycloak Admin Client (`org.keycloak:keycloak-admin-client:26.0.8`)
- **Khác**
  - QueryDSL JPA (filter linh hoạt)
  - Springdoc OpenAPI (Swagger UI tại `/swagger-ui`)
  - MinIO (lưu trữ file, avatar)
  - Spring AI (chat, RAG – không bắt buộc cho dòng chính)

### 2. Các module / package chính

```text
com.sba.ssos
  configuration/       # ApplicationProperties, KeycloakAdminConfig, MinioConfig, ...
  controller/
    user/              # UserController, KeycloakUserController, AdminUserController
    order/, review/    # API order, review...
    advice/            # GlobalExceptionHandler
  dto/
    request/           # DTO request cho user, product, review,...
    response/          # DTO response, PageResponse, UserResponse, AdminUserResponse...
  entity/              # User, Customer, Shoe, Order, Review,...
  enums/               # UserRole, UserStatus, ShoeStatus, ...
  repository/          # UserRepository, CustomerRepository, ShoeRepository, ...
  security/            # SecurityConfig, JwtConverter, CorsConfig
  service/
    user/              # UserService, AdminUserService
    keycloak/          # KeycloakAdminService (giao tiếp Keycloak Admin API)
    customer/, order/  # Business logic các domain khác
  utils/               # DateUtils, LocaleUtils, ...
```

#### 2.1. User & Keycloak

- **`UserService`**
  - Lấy / cập nhật profile user hiện tại (`/users/me`)
  - Upload avatar (MinIO)
  - Nhận webhook từ Keycloak khi user mới đăng ký (qua `KeycloakUserController`) và tạo bản ghi `User` + `Customer`.
- **`KeycloakAdminService`**
  - Đóng vai trò gateway tới Keycloak Admin API:
    - Tạo user (`createUser`)
    - Đặt mật khẩu tạm (`setTemporaryPassword`)
    - Gán / thay thế realm role (`assignRealmRole`, `replaceRealmRole`)
    - Bật / tắt user (`setUserEnabled`)
    - Xoá user (`deleteUser`)
- **`AdminUserService` + `AdminUserController`**
  - Expose API quản lý user cho admin:
    - `GET /api/admin/users` – phân trang + filter `search`, `role`, `status` (trả về `PageResponse<AdminUserResponse>`)
    - `POST /api/admin/users` – tạo admin/manager mới (Keycloak + DB)
    - `PATCH /api/admin/users/{keycloakId}/role` – đổi role
    - `PATCH /api/admin/users/{keycloakId}/status` – suspend/activate
    - `DELETE /api/admin/users/{keycloakId}` – xóa hoàn toàn

#### 2.2. Response wrapper

- Tất cả API trả về `ResponseGeneral<T>` (status, message, data, timestamp).
- Với danh sách phân trang, dùng `PageResponse<T>` để bọc Spring `Page<T>`:

```java
public record PageResponse<T>(
    List<T> content,
    int number,
    int size,
    long totalElements,
    int totalPages,
    boolean first,
    boolean last
) {
  public static <T> PageResponse<T> from(Page<T> page) { ... }
}
```

Điều này giúp tránh expose trực tiếp `org.springframework.data.domain.Page` ra JSON (khó serialize) và đồng bộ với DTO phía frontend.

### 3. Cấu hình & biến môi trường quan trọng

#### 3.1. Database & server (`src/main/resources/application.yml`)

- PostgreSQL:
  - `POSTGRESQL_HOST` (mặc định `localhost`)
  - `POSTGRESQL_PORT` (mặc định `5432`)
  - `POSTGRESQL_NAME` (mặc định `ssos`)
  - `POSTGRESQL_USERNAME`, `POSTGRESQL_PASSWORD`
- Server:
  - Port: `8088`
  - Liquibase bật mặc định (`spring.liquibase.enabled: true`)

#### 3.2. Keycloak (`application-properties.yml`)

- `KEYCLOAK_REALM` – tên realm (mặc định `ssos-realm`)
- `KEYCLOAK_CLIENT_ID` – client Id (ví dụ `ssos-app`)
- `KEYCLOAK_HOST` – URL Keycloak (ví dụ `http://localhost:8080`)
- `KEYCLOAK_ADMIN_CLIENT_ID` – admin client (mặc định `admin-cli`)
- `KEYCLOAK_ADMIN_USERNAME`, `KEYCLOAK_ADMIN_PASSWORD` – tài khoản admin dùng cho Keycloak Admin Client
- `KEYCLOAK_TOKEN_URL` – endpoint lấy token nếu cần

#### 3.3. MinIO

- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_BUCKET` (mặc định `ssos-images`)

### 4. Security & phân quyền

- Config tại `SecurityConfig` + `ApplicationProperties.SecurityProperties`:
  - `admin-endpoints` – các endpoint yêu cầu `ROLE_ADMIN` (bao gồm `/api/admin/users/**`, brands, categories, shoes, orders admin, AI APIs)
  - `customer-endpoints` – các endpoint yêu cầu `ROLE_CUSTOMER` (`/users/me`, wishlist, cart, orders, chat,...)
  - `webhook-endpoints` – endpoint webhook (ví dụ `/api/payments/sepay/hook`) yêu cầu `ROLE_SEPAY_WEBHOOK`
  - `public-endpoints` – endpoint public (catalog, files GET, webhook đăng ký user từ Keycloak)

### 5. Cách chạy backend

Yêu cầu: Java 21, PostgreSQL đang chạy, Keycloak đã cấu hình realm + client phù hợp.

```bash
# 1. Chạy PostgreSQL & Keycloak (tự chuẩn bị hoặc dùng docker compose của bạn)

# 2. Thiết lập biến môi trường (ví dụ trên Windows PowerShell)
$env:POSTGRESQL_HOST="localhost"
$env:POSTGRESQL_USERNAME="postgres"
$env:POSTGRESQL_PASSWORD="123456"
$env:KEYCLOAK_HOST="http://localhost:8080"
$env:KEYCLOAK_REALM="ssos-realm"
$env:KEYCLOAK_CLIENT_ID="ssos-app"
$env:KEYCLOAK_ADMIN_USERNAME="admin"
$env:KEYCLOAK_ADMIN_PASSWORD="admin"

# 3. Chạy ứng dụng
./gradlew bootRun
```

Backend sẽ lắng nghe tại `http://localhost:8088` và expose Swagger UI tại `http://localhost:8088/swagger-ui`.

