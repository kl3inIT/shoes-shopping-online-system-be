# Exception Handling (Backend)

Tài liệu này mô tả **chuẩn throw & handle exception** trong backend `shoes-shopping-online-system-be`.

## Mục tiêu

- **Thống nhất response lỗi** theo RFC 7807 (`ProblemDetail`)
- **Hỗ trợ i18n** thông qua `messageKey` + `MessageSource` (`LocaleUtils`)
- **Tách bạch**:
  - Lỗi domain/app → throw `BaseException` (và các subclass)
  - Lỗi framework (validation, JSON parse, security) → handle riêng trong `GlobalExceptionHandler`

## Tổng quan luồng xử lý

2. `GlobalExceptionHandler` bắt exception và trả về `ProblemDetail`.
3. `detail` được build bằng `LocaleUtils.get(messageKey, args...)`.

## 1) BaseException & các subclass

**File:** `src/main/java/com/sba/ssos/exception/base/BaseException.java`

- `BaseException` lưu:
  - `HttpStatus status`
  - `messageKey` (nằm ở `getMessage()`)
  - `params` (Map) dùng làm args để format message i18n
- `BaseException.params(...)` dùng `LinkedHashMap` để **giữ thứ tự** → đảm bảo format đúng với placeholder dạng `{0}`, `{1}`, ...

### Danh sách exception base

| Class | HTTP | Khi nào dùng |
|------|------|--------------|
| `BadRequestException` | 400 | Request invalid/logic sai |
| `UnauthorizedException` | 401 | Chưa login / token invalid |
| `ForbiddenException` | 403 | Có login nhưng không đủ quyền |
| `NotFoundException` | 404 | Resource không tồn tại |
| `ConflictException` | 409 | Trùng/Conflict dữ liệu |
| `UnprocessableEntityException` | 422 | Validation/semantic error |
| `InternalServerErrorException` | 500 | Lỗi server không mong đợi |

### Có cần handler riêng cho exception “nhỏ” không?

Không cần.

Ví dụ `UserNotFoundException extends NotFoundException` vẫn được bắt bởi:

- `@ExceptionHandler(BaseException.class)`

## 2) i18n message keys & placeholders

**Folder i18n:** `src/main/resources/i18n/`

- `messages_en.properties`
- `messages_vi.properties`

### Quy ước placeholders

Spring `MessageSource` dùng **MessageFormat**:
- Dùng `{0}`, `{1}`, `{2}`... (positional placeholders)
- Không dùng `{resourceName}` kiểu named placeholder

Ví dụ:

```properties
error.resource.not.found={0} with ID {1} not found
error.jwt.invalid_azp=Invalid authorized party (azp). Expected [{0}] but got [{1}]
```

### Cách truyền params

```java
throw new NotFoundException("User", userId);
// NotFoundException sẽ dùng messageKey error.resource.not.found
// và params: ("resourceName","User","resourceId",userId)
// → args order: User, userId
```

**Lưu ý:** `GlobalExceptionHandler` đang dùng `ex.getParams().values().toArray()` làm args, nên thứ tự `values()` phải ổn định → vì vậy `params()` dùng `LinkedHashMap`.

## 3) GlobalExceptionHandler (thống nhất response)

**File:** `src/main/java/com/sba/ssos/controller/advice/GlobalExceptionHandler.java`

### Những case được handle

- **`BaseException`**: toàn bộ exception domain/app
- **`MethodArgumentNotValidException`**: lỗi `@Valid`, trả 400 + map `errors` theo field
- **`HttpMessageNotReadableException`**: body JSON sai format, trả 400
- **`AccessDeniedException`**: 403 từ Spring Security
- **`AuthenticationException` / `JwtException`**: 401 từ Spring Security/JWT
- **Fallback `Exception`**: 500

### Format response (ProblemDetail)

Ví dụ `NotFoundException`:

```json
{
  "type": "about:blank",
  "title": "Not Found",
  "status": 404,
  "detail": "User with ID 123 not found",
  "instance": "/user/me",
  "messageKey": "error.resource.not.found",
  "params": {
    "resourceName": "User",
    "resourceId": "123"
  }
}
```

Validation (`MethodArgumentNotValidException`) có thêm:

```json
{
  "status": 400,
  "detail": "Dữ liệu không hợp lệ",
  "errors": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu quá ngắn"
  }
}
```

## 4) Best practices (áp dụng trong project này)

- **Chỉ cần 1 handler cho BaseException** (không viết handler riêng cho từng domain exception nhỏ).
- **Viết handler riêng cho framework exceptions** (validation, json parse, security).
- **Luôn dùng messageKey** (không hardcode message) để đảm bảo i18n.
- **Dùng `{0}`, `{1}`** trong properties để format args.

