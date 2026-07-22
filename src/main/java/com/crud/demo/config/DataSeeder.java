package com.crud.demo.config;

import com.crud.demo.EmployeeRepository;
import com.crud.demo.entity.Employee;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataSeeder implements CommandLineRunner {
    private final EmployeeRepository employeeRepository;

    public DataSeeder(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public void run(String... args) {
        if (employeeRepository.count() == 0) {
            employeeRepository.save(new Employee(null, "Naveen", "Developer", new BigDecimal("12")));
            employeeRepository.save(new Employee(null, "Naveen2", "Developer", new BigDecimal("24")));
        }
    }

}
