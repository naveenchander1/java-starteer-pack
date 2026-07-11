package com.crud.demo.service;

import com.crud.demo.exception.EmployeeNotFoundException;
import com.crud.demo.model.Employee;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {
    final List<Employee> employees = new ArrayList<>();

    public EmployeeService(){
        this.employees.add(new Employee(1L, "Naveen", "Developer", new BigDecimal("12")));
        this.employees.add(new Employee(2L, "Naveen2", "Developer", new BigDecimal("24")));
    }

    public List<Employee> getAllEmployees(){
      return this.employees;
    }

    public Employee getEmployee(Long id){
        for (Employee employee : employees) {
            if(employee.getId().equals(id)){
                return employee;
            }
        }
        throw new EmployeeNotFoundException("Employee not found with id: " + id);
    }

    public List<Employee> addEmployee(Employee employee) {
        this.employees.add(employee);
        return this.employees;
    }

    public Employee updateEmployee(Employee updatedEmployee, Long id) {
        for (Employee employee : employees) {
            if (employee.getId().equals(id)) {
                employee.setName(updatedEmployee.getName());
                employee.setDepartment(updatedEmployee.getDepartment());
                // Update any other fields you have

                return employee;
            }
        }

        throw new EmployeeNotFoundException("Employee not found with id: " + id);
    }

    public boolean deleteEmployee(Long id){
        return employees.removeIf(employee -> employee.getId().equals(id));
    }

    public List<Employee> searchEmployees (String department) {
      List<Employee> result = new ArrayList<>();
      for(Employee employee: employees){
          if(employee.getDepartment().equalsIgnoreCase(department)){
              result.add(employee);
          }
      }
      return result;
    }

}
