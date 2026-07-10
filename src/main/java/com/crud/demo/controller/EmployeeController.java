package com.crud.demo.controller;

import com.crud.demo.model.Employee;
import com.crud.demo.service.EmployeeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EmployeeController {
    private final EmployeeService employeeService;

    public EmployeeController(EmployeeService employeeService) {
        this.employeeService = employeeService;
    }

    @GetMapping("/employees")
    public List<Employee> getAllEmployees() {
        return employeeService.getAllEmployees();
    }

    @GetMapping("/employees/{id}")
    public Employee getEmployee(@PathVariable int id) {
        return employeeService.getEmployee(id);
    }

    @PostMapping("/employees")
    public List<Employee> postEmployee (@Valid @RequestBody Employee employee) {
        return employeeService.addEmployee(employee);
    };

    @PutMapping("/employees/{id}")
    public Employee updateEmployee(@RequestBody Employee employee, @PathVariable int id) {
        return employeeService.updateEmployee(employee, id);
    };

    @DeleteMapping("/employeee/{id}")
    public boolean deleteEmployee(@PathVariable int id) {
        return employeeService.deleteEmployee(id);
    };

    @GetMapping("/employees/search")
    public List<Employee> searchEmployees (@RequestParam String department ) {
        return employeeService.searchEmployees(department);
    };

}
