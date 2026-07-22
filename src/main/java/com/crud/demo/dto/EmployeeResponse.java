package com.crud.demo.dto;

import com.crud.demo.entity.Employee;
import lombok.experimental.NonFinal;

import java.math.BigDecimal;

public class EmployeeResponse {
    private final Long id;
    private final String name;
    private final String department;
    private final BigDecimal salary;

    public EmployeeResponse(Long id, String name, String department, BigDecimal salary){
        this.id = id;
        this.name = name;
        this.department = department;
        this.salary = salary;
    }

    public static EmployeeResponse from(Employee employee) {
        return new EmployeeResponse(
                employee.getId(),
                employee.getName(),
                employee.getDepartment(),
                employee.getSalary()
        );
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDepartment() {
        return department;
    }

    public BigDecimal getSalary() {
        return salary;
    }

}
