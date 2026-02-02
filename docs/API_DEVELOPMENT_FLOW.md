# API Development Flow (Backend)

Tài liệu này mô tả **luồng phát triển một API mới** trong backend `shoes-shopping-online-system-be` theo chuẩn hiện tại của dự án:

- Success response: `ResponseGeneral<T>`
- Error response: RFC 7807 `ProblemDetail` (qua `GlobalExceptionHandler`)
- Domain errors: `BaseException` + i18n message keys
- Mapping: MapStruct (`@Mapper(componentModel = "spring")`)
- Auth: Spring Security Resource Server (JWT)

---

## 0) Checklist nhanh

- **Route**: đặt đúng path trong controller
- **DTO**: request/response tách khỏi entity
- **Mapper**: MapStruct để map entity ↔ DTO
- **Service**: chứa business logic + transaction
- **Repository**: JPA query cần thiết
- **Exception**: domain-specific (vd `UserNotFoundException`) hoặc base (`NotFoundException`)
- **i18n**: thêm message keys vào `src/main/resources/i18n/messages_*.properties`
- **Security**: endpoint public hay protected? (update `application.yml` nếu public)
- **Test**: smoke test bằng curl/Postman

---

## 1) Thiết kế API (contract)

Trước khi code, xác định:

- **HTTP method**: GET/POST/PUT/DELETE
- **Path**: ví dụ `/user/{keycloakId}`
- **Auth**: cần token hay public
- **Response type**:
  - Success: `ResponseGeneral<T>`
  - Error: `ProblemDetail` (tự động bởi handler)
- **Status code**: 200/201/204...

Ví dụ:

- `GET /user/{keycloakId}` (protected)
  - 200: `ResponseGeneral<UserResponse>`
  - 404: `ProblemDetail` (NotFound)

---

## 2) Tạo DTO (Request/Response)

Đặt DTO trong package `com.sba.ssos.dto.*` (project bạn đang dùng dạng `dto/response/...`).

Ví dụ response:

```java
// com.sba.ssos.dto.response.user.UserResponse
public record UserResponse(
    UUID keycloakId,
    String username,
    String email,
    String phoneNumber,
    LocalDate dateOfBirth,
    String avatarUrl,
    Instant lastSeenAt
) {}
```

---

## 3) Tạo Mapper (MapStruct)

Tạo mapper trong `com.sba.ssos.mapper`:

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
  UserResponse toResponse(User user);
}
```

**Lưu ý:** MapStruct đã được add trong `build.gradle`:

- `implementation 'org.mapstruct:mapstruct:1.6.3'`
- `annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'`

---

## 4) Repository

Thêm method query trong repository nếu cần:

```java
public interface UserRepository extends JpaRepository<User, UUID> {
  Optional<User> findByKeycloakId(UUID keycloakId);
}
```

---

## 5) Exception (domain + i18n)

### 5.1 Base exceptions

Project dùng `BaseException` và các subclass (`NotFoundException`, `ConflictException`, ...).

### 5.2 Domain-specific exception (khuyến nghị)

Ví dụ:

```java
public class UserNotFoundException extends NotFoundException {
  public UserNotFoundException(UUID userId) {
    super("User", userId);
  }
}
```

**Không cần** viết handler riêng trong `GlobalExceptionHandler` cho exception con này, vì đã có:

- `@ExceptionHandler(BaseException.class)`

---

## 6) Service layer (business logic)

Service là nơi:

- gọi repository
- throw domain exceptions
- đảm bảo transaction boundaries (`@Transactional`)
- map entity → response DTO bằng mapper

Ví dụ:

```java
@Transactional(readOnly = true)
public UserResponse getUserByKeycloakId(UUID keycloakId) {
  User user = userRepository.findByKeycloakId(keycloakId)
      .orElseThrow(() -> new UserNotFoundException(keycloakId));
  return userMapper.toResponse(user);
}
```

---

## 7) Controller layer (routing + response)

Controller:

- gọi service
- trả `ResponseGeneral<T>` cho success
- không cần try/catch cho domain errors (để handler xử lý)

Ví dụ:

```java
@GetMapping("/{keycloakId}")
public ResponseGeneral<UserResponse> getByKeycloakId(@PathVariable UUID keycloakId) {
  UserResponse data = userService.getUserByKeycloakId(keycloakId);
  return ResponseGeneral.ofSuccess(localeUtils.get("success.user.fetched"), data);
}
```

---

## 8) i18n messages

### 8.1 Location

`src/main/resources/i18n/messages_en.properties`  
`src/main/resources/i18n/messages_vi.properties`

### 8.2 Placeholder rule

**Dùng `{0}`, `{1}`, ...** (MessageFormat).

Ví dụ:

```properties
success.user.fetched=User fetched successfully.
success.user.synced=User synced successfully.

error.resource.not.found={0} with ID {1} not found
```

---

## 9) Security (public vs protected)

Mặc định theo `SecurityConfig`:

- Nếu không nằm trong `public-endpoints` / `public-urls` → **authenticated**

Nếu muốn endpoint public:

1. Update `src/main/resources/application.yml`
2. Add vào `application-properties.security-properties.public-endpoints` (method + path)

Ví dụ:

```yml
public-endpoints:
  - { method: GET, path: /products }
```

---

## 10) Error responses (ProblemDetail)

Lỗi domain (BaseException) và lỗi framework (validation/json/security) được trả theo `ProblemDetail`.

Ví dụ NotFound:

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "User with ID 123 not found",
  "instance": "/user/123",
  "messageKey": "error.resource.not.found",
  "params": { "resourceName": "User", "resourceId": "123" }
}
```

---

## 11) Smoke test nhanh

### 11.1 Protected endpoint

- Lấy access token từ FE/OIDC flow
- Gọi:

```bash
curl -H "Authorization: Bearer <token>" http://localhost:8088/user/<keycloakId>
```

### 11.2 Public endpoint

```bash
curl http://localhost:8088/test/free
```

---

## 12) Quy ước đặt tên (gợi ý)

- Controller: `{Domain}Controller`
- Service: `{Domain}Service`
- Repository: `{Domain}Repository`
- Mapper: `{Domain}Mapper`
- Exception: `{Domain}{Problem}Exception` (vd `UserNotFoundException`)
- i18n keys:
  - success: `success.<domain>.<action>`
  - error: `error.<domain>.<problem>` hoặc dùng generic `error.resource.*`

