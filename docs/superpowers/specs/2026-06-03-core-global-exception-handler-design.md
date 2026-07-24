# Core Global Exception Handler Design

## Context

The `core` module already provides a web response contract through `R<T>`, `RResponseBodyAdvice`, and `CoreWebAutoConfiguration`. Controller responses are wrapped as:

```json
{
  "code": 0,
  "message": "success",
  "data": {}
}
```

The new exception handling should live in the same web package and auto-configuration path so applications depending on `:core` receive the behavior without local wiring.

## Selected approach

Use a centralized Spring MVC `@RestControllerAdvice` plus small business exception types.

This is preferred because it preserves the existing `R<T>` contract, keeps the implementation understandable, and avoids introducing a separate Problem Details format or an oversized error-code registry before the project has enough domain-specific errors to justify one.

## Error code policy

Successful responses keep `code = 0`.

For common framework exceptions, `R.code` is the HTTP status code:

- Bad request: `400`
- Unauthorized: `401`
- Forbidden: `403`
- Not found: `404`
- Conflict: `409`
- Unsupported method: `405`
- Unsupported media type: `415`
- Internal server error: `500`

Business exceptions may override `code` when the domain needs a stable business error code. If no custom business code is supplied, the code defaults to the exception HTTP status value.

## Components

### `R`

Add an error factory:

```kotlin
fun error(code: Int, message: String): R<Nothing>
```

The factory returns an `R` with `data = null`, which is omitted from JSON by the existing `@JsonInclude` policy.

### Business exceptions

Add `BusinessException` with immutable fields:

- `code: Int`
- `status: HttpStatus`
- inherited `message`

Add common subclasses:

- `BadRequestException`
- `UnauthorizedException`
- `ForbiddenException`
- `NotFoundException`
- `ConflictException`

Each subclass defaults `code` to the matching HTTP status and accepts a caller-provided safe message.

### `GlobalExceptionHandler`

Add a `@RestControllerAdvice` that pins the HTTP status per exception type
and writes the unified `R<Nothing>` body. The handler is the **only**
component in the project permitted to return `ResponseEntity`; business
controllers must not. `RResponseBodyAdvice` skips `ResponseEntity` return
types, so the body is written verbatim. Handlers cover:

- `BusinessException`
- `MethodArgumentNotValidException`
- `BindException`
- `ConstraintViolationException`
- `HttpMessageNotReadableException`
- `MissingServletRequestParameterException`
- `MethodArgumentTypeMismatchException`
- `NoHandlerFoundException`
- `HttpRequestMethodNotSupportedException`
- `HttpMediaTypeNotSupportedException`
- `ResponseStatusException`
- fallback `Exception`

Validation messages should be deterministic and safe. Field validation reports the first field error as `field message` when available; otherwise it falls back to a generic `bad request` message. Unknown exceptions return `internal server error` and do not expose implementation details.

### Auto-configuration

Update `CoreWebAutoConfiguration` to register `GlobalExceptionHandler` next to `RResponseBodyAdvice`.

## Testing

Use Spring MVC tests in `core` with real controllers and `MockMvc`. Tests should cover:

- `BusinessException` status/code/message mapping.
- Custom business error code mapping.
- Request body validation failure returns 400.
- Missing servlet request parameter returns 400.
- Method argument type mismatch returns 400.
- Malformed JSON body returns 400.
- Unsupported HTTP method returns 405.
- `ResponseStatusException` returns its status and reason.
- Unhandled exceptions return 500 with a safe generic message.

## Non-goals

- Authentication or authorization implementation.
- A complete enterprise error-code registry.
- Localization of error messages.
- Logging policy changes.
- Replacing the existing `R<T>` response contract with Problem Details.

## Acceptance criteria

- `core` exposes common business exception classes.
- `core` auto-configures a global exception handler.
- Common Spring MVC and validation exceptions return consistent `R` error responses.
- Unknown exceptions return a safe 500 response without leaking internal details.
- Existing response wrapping behavior remains intact.
- Relevant `core` tests pass.
