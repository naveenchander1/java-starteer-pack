# Plan: Production-grade error handling hardening

## Context

Testing `GET /employeesd` (a typo/unmapped route) exposed two gaps between what we have and what's safe to ship:

1. **Stack traces are leaking to the client.** Any request that isn't caught by our `GlobalExceptionHandler` (e.g. `NoResourceFoundException` for an unmapped URL) falls through to Spring Boot's own default error handler, which returned a JSON body containing a full `trace` field — internal file paths, library versions, class names. That's real reconnaissance information for anyone probing the API and must never reach a client in production.
2. **Not every exception is centralized yet.** `GlobalExceptionHandler` only knows about `EmployeeNotFoundException` and `MethodArgumentNotValidException`. Anything else (unmapped routes, a future `NullPointerException`, a bug) skips our consistent `ErrorResponse` shape entirely and falls back to Spring's raw default handler.

**Goal:** nothing unexpected should ever return a raw stack trace to the client. Every response — expected error, unexpected bug, or unmapped route — should come back in the same `ErrorResponse` shape, while the *real* error detail is logged server-side where you can still debug it.

**Main tradeoff to keep in mind:** hiding details from the client makes debugging harder unless paired with real server-side logging. That's exactly what step 2 below adds — you're not losing the information, you're just moving it from "sent to attacker" to "written to your logs."

---

## Step 1 — Stop leaking stack traces via Spring's default handler

File: `src/main/resources/application.properties`

Add:
```properties
server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
```

Full file after the change:
```properties
spring.application.name=demo
server.port=8080

server.error.include-stacktrace=never
server.error.include-message=never
server.error.include-binding-errors=never
```

Why: this is Spring Boot's own built-in default `/error` handler (`BasicErrorController`) — the thing that produced the `timestamp`/`status`/`error`/`trace`/`message`/`path` JSON you saw. These properties tell it to never include the stack trace, the raw exception message, or field-binding errors in its response, regardless of what triggers it. This is a safety net that protects you even for exceptions your own code hasn't thought to handle yet — defense in depth, on top of step 2.

---

## Step 2 — Add a catch-all handler to `GlobalExceptionHandler`

File: `src/main/java/com/crud/demo/exception/GlobalExceptionHandler.java`

Two more `@ExceptionHandler` methods, so *every* exception type ends up producing your consistent `ErrorResponse` JSON instead of falling through to Spring's default page:

1. **`NoResourceFoundException`** — handles the "unmapped URL" case specifically (like `/employeesd`), returning the semantically correct `404` (a typo'd/unknown route is a client error, not a server error).
2. **`Exception`** (catch-all) — handles anything truly unanticipated, logs the *real* exception server-side with its full stack trace, and returns the client a generic `500` with no internal detail.

```java
package com.crud.demo.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException ex) {
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(HttpStatus.NOT_FOUND.value(), "No such route: " + ex.getResourcePath()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR.value(), "Something went wrong"));
    }
}
```

Notes:
- **Ordering doesn't matter.** Spring always picks the *most specific* matching `@ExceptionHandler` for a thrown exception, so `EmployeeNotFoundException` will still hit `handleNotFound`, not the generic `handleUnexpected`, even though `Exception` is a superclass of everything. You don't need to worry about method order in the file.
- **Why log only in the catch-all, not the others:** `EmployeeNotFoundException` and validation failures are *expected*, routine client errors (wrong id, bad input) — logging every one of those at `ERROR` level would flood your logs with noise. The catch-all is specifically for the "this should never happen" case, which is exactly when you want a loud server-side log with the full stack trace (`log.error("...", ex)` includes it automatically).
- `log` uses SLF4J (`org.slf4j`), the standard logging facade Spring Boot wires up by default — you don't need to add any dependency for this.

---

## Step 3 — Verify

1. `.\mvnw.cmd -q compile`
2. `.\mvnw.cmd spring-boot:run`
3. Exercise these cases:

   | Request | Before | After (expected) |
   |---|---|---|
   | `GET /employeesd` (typo route) | `404` with full stack `trace` field | `404` — `{"status":404,"message":"No such route: employeesd"}`, no trace |
   | `GET /employees/999` | `404`, clean body (already working) | unchanged |
   | `POST /employees` with a body that still trips the `salary`/`@NotBlank` bug | `500` with stack trace | `500` — `{"status":500,"message":"Something went wrong"}`, no trace. Check the app console: you should see the full `UnexpectedTypeException` logged there via `log.error`, proving the real detail wasn't lost, just moved server-side. |
   | `GET /employees/1` | `200`, employee JSON | unchanged |

If all four match, the app now has a single, consistent, safe error contract for every failure mode.
