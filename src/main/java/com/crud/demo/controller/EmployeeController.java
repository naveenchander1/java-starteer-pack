package com.crud.demo.controller;

import com.crud.demo.dto.EmployeeRequest;
import com.crud.demo.dto.EmployeeResponse;
import com.crud.demo.entity.Employee;
import com.crud.demo.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/employees")
public class EmployeeController {

    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {
        return employeeService.getAllEmployees().stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public EmployeeResponse getEmployee(@PathVariable Long id) {
        return EmployeeResponse.from(employeeService.getEmployee(id));
    }

    @PostMapping
    public ResponseEntity<EmployeeResponse> postEmployee(@Valid @RequestBody EmployeeRequest request) {
        Employee saved = employeeService.addEmployee(toEntity(request));
        return ResponseEntity
                .created(URI.create("/employees/" + saved.getId()))
                .body(EmployeeResponse.from(saved));
    }

    @PutMapping("/{id}")
    public EmployeeResponse updateEmployee(@Valid @RequestBody EmployeeRequest request, @PathVariable Long id) {
        Employee updated = employeeService.updateEmployee(toEntity(request), id);
        return EmployeeResponse.from(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<EmployeeResponse> searchEmployees(@RequestParam String department) {
        return employeeService.searchEmployees(department).stream()
                .map(EmployeeResponse::from)
                .collect(Collectors.toList());
    }

    private Employee toEntity(EmployeeRequest request) {
        Employee employee = new Employee();
        employee.setName(request.getName());
        employee.setDepartment(request.getDepartment());
        employee.setSalary(request.getSalary());
        return employee;
    }
}