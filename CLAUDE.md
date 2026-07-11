# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project overview

A minimal Spring Boot 4.1.0 REST API (Java 17, Maven) exposing CRUD endpoints for an in-memory `Employee` list. There is no database — `EmployeeService` stores employees in a plain `ArrayList` seeded with two sample records at construction time, so all data resets on restart.

## Commands

Use the Maven wrapper (no local Maven install required). On Windows use `mvnw.cmd`; the examples below use the Unix wrapper syntax.

```
./mvnw spring-boot:run          # run the app (starts on port 8080)
./mvnw compile                  # compile only
./mvnw test                     # run all tests
./mvnw test -Dtest=DemoApplicationTests#contextLoads   # run a single test method
./mvnw package                  # build the jar (target/*.jar)
```

## Architecture

Standard three-layer Spring MVC structure under `src/main/java/com/crud/demo/`:

- `controller/EmployeeController.java` — `@RestController` mapping HTTP verbs to `/employees` routes. No `@RequestMapping` base path is set at the class level; each method declares its own path.
- `service/EmployeeService.java` — `@Service` holding the in-memory `List<Employee>` and all business logic (lookup, add, update, delete, search by department). Not found lookups throw `ResponseStatusException(HttpStatus.NOT_FOUND)`.
- `model/Employee.java` — plain POJO (no Lombok annotations currently applied, despite Lombok being a dependency) with manual getters/setters and `jakarta.validation` constraints (`@NotBlank`, `@Positive`) used by `@Valid` on the POST endpoint.
- `model/Positive.java` — an empty, unused custom `@interface Positive`. `Employee.id` actually uses `jakarta.validation.constraints.Positive`, not this one — don't assume this custom annotation is wired into validation.

Known quirks to be aware of when editing this code:
- The DELETE route is `/employeee/{id}` (three e's), inconsistent with the other `/employees/...` routes.
- `updateEmployee` only updates `name` and `department`; `salary` and `id` are left untouched even if present in the request body.
- `PUT /employees/{id}` returns `null` (no error status) when the id isn't found, rather than a 404.
