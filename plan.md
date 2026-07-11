# Plan: Centralized error handling for EmployeeService

## Context

Right now error handling is inconsistent and scattered:
- `EmployeeService.getEmployee` throws `ResponseStatusException(HttpStatus.NOT_FOUND, ...)` inline.
- `EmployeeService.updateEmployee` silently returns `null` when the id isn't found — the controller then returns HTTP 200 with an empty body instead of a 404. That's a real bug: a client can't tell "updated to null" from "not found".
- Validation failures from `@Valid` on `POST /employees` fall through to Spring's default exception handling, which isn't controlled by us and isn't consistent with how we report the "not found" case.

**Goal:** introduce one centralized place (`@RestControllerAdvice`) that maps well-defined exceptions to consistent JSON error responses, and make the service layer *throw* exceptions instead of returning `null` or building ad-hoc responses. This is the Spring Boot equivalent of Express error-handling middleware — instead of every route/service formatting its own errors, exceptions bubble up and get formatted in one place.

**Scope:** also centralize `@Valid` validation errors, not just the "not found" case.

---

## Step 1 — Create the exception package

Create a new folder: `src/main/java/com/crud/demo/exception/`

### 1a. `EmployeeNotFoundException.java`

A simple unchecked exception — just a message-carrying `RuntimeException` subclass:

```java
package com.crud.demo.exception;

public class EmployeeNotFoundException extends RuntimeException {
    public EmployeeNotFoundException(String message) {
        super(message);
    }
}
```

Why `RuntimeException` and not a checked exception: Spring's exception handling (and this codebase generally) uses unchecked exceptions so callers aren't forced to declare/catch them everywhere — the `@RestControllerAdvice` catches them centrally instead.

### 1b. `ErrorResponse.java`

A record DTO for the JSON error body. Java 17 records are a good fit here — immutable, no boilerplate getters:

```java
package com.crud.demo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String message, Map<String, String> fieldErrors) {
    public ErrorResponse(int status, String message) {
        this(status, message, null);
    }
}
```

`fieldErrors` stays `null` (and is omitted from the JSON, thanks to `@JsonInclude`) for simple not-found errors, and is populated for validation errors.

### 1c. `GlobalExceptionHandler.java`

`@RestControllerAdvice` is auto-detected by Spring and applied to every `@RestController` in the app — no manual registration needed, similar to how Express applies error middleware globally once you `app.use()` it.

```java
package com.crud.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(EmployeeNotFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(HttpStatus.BAD_REQUEST.value(), "Validation failed", fieldErrors));
    }
}
```

Notes:
- `MethodArgumentNotValidException` is the exception Spring throws automatically when `@Valid @RequestBody` fails — you don't throw this yourself, it happens before your controller method body even runs.
- `ex.getBindingResult().getFieldErrors()` gives you one `FieldError` per failed `@NotBlank`/`@Positive`/etc. constraint; `error.getField()` is the property name (e.g. `"name"`), `error.getDefaultMessage()` is the `message = "..."` you set on the annotation.

---

## Step 2 — Update `EmployeeService.java`

File: `src/main/java/com/crud/demo/service/EmployeeService.java`

1. Remove these two now-unused imports:
   ```java
   import org.springframework.http.HttpStatus;
   import org.springframework.web.server.ResponseStatusException;
   ```
2. Add:
   ```java
   import com.crud.demo.exception.EmployeeNotFoundException;
   ```
3. In `getEmployee`, replace:
   ```java
   throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found");
   ```
   with:
   ```java
   throw new EmployeeNotFoundException("Employee not found with id: " + id);
   ```
4. In `updateEmployee`, replace:
   ```java
   return null; // or throw an exception
   ```
   with:
   ```java
   throw new EmployeeNotFoundException("Employee not found with id: " + id);
   ```

**No changes needed in `EmployeeController.java`** — exceptions thrown from the service propagate up through the controller automatically and get caught by `GlobalExceptionHandler`.

**Not in scope for this step:** `deleteEmployee` keeps returning `boolean` as-is. Returning `false` for "nothing deleted" vs. throwing is a separate design decision from the null-vs-throw bug being fixed here.

---

## Step 3 — Verify

1. Compile: `.\mvnw.cmd -q compile`
2. Run: `.\mvnw.cmd spring-boot:run`
3. Exercise the endpoints (PowerShell `Invoke-RestMethod`, `curl`, or Postman):

   | Request | Before | After (expected) |
   |---|---|---|
   | `GET /employees/999` | 404, plain text | `404` — `{"status":404,"message":"Employee not found with id: 999"}` |
   | `PUT /employees/999` with a JSON body | `200` with empty/null body | `404` — `{"status":404,"message":"Employee not found with id: 999"}` |
   | `POST /employees` with missing `name` | Spring's default validation error shape | `400` — `{"status":400,"message":"Validation failed","fieldErrors":{"name":"Name is required"}}` |
   | `GET /employees/1` | 200, employee JSON | unchanged — 200, employee JSON |

If step 3's results match, the fix is complete.
