package com.crud.demo.service;

import com.crud.demo.EmployeeRepository;
import com.crud.demo.entity.Employee;
import com.crud.demo.exception.EmployeeNotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class EmployeeService {
    private final EmployeeRepository employeeRepository;

    public EmployeeService(EmployeeRepository employeeRepository){
        this.employeeRepository = employeeRepository;
    }

    public List<Employee> getAllEmployees(){
        return employeeRepository.findAll();
    }

    public Employee getEmployee(Long id){
        return employeeRepository.findById(id).orElseThrow(() -> new EmployeeNotFoundException("Employee not found with id:"+id));

//        for (Employee employee : employees) {
//            if(employee.getId().equals(id)){
//                return employee;
//            }
//        }
//        throw new EmployeeNotFoundException("Employee not found with id: " + id);
    }

    public Employee addEmployee(Employee employee) {
        return employeeRepository.save(employee);
        //        this.employees.add(employee);
//        return this.employees;
    }

    public Employee updateEmployee(Employee updatedEmployee, Long id) {

        Employee existingEmployee = getEmployee(id);
        existingEmployee.setName(updatedEmployee.getName());
        existingEmployee.setDepartment(updatedEmployee.getDepartment());
        existingEmployee.setSalary(updatedEmployee.getSalary());
        return employeeRepository.save(existingEmployee);

//        for (Employee employee : employees) {
//            if (employee.getId().equals(id)) {
//                employee.setName(updatedEmployee.getName());
//                employee.setDepartment(updatedEmployee.getDepartment());
//                // Update any other fields you have
//
//                return employee;
//            }
//        }
//
//        throw new EmployeeNotFoundException("Employee not found with id: " + id);
    }

    public void deleteEmployee(Long id){
//        return employees.removeIf(employee -> employee.getId().equals(id));
        if (!employeeRepository.existsById(id)) {
            throw new EmployeeNotFoundException("Employee not found with id: " + id);
        }
        employeeRepository.deleteById(id);
    }

    public List<Employee> searchEmployees (String department) {
//      List<Employee> result = new ArrayList<>();
//      for(Employee employee: employees){
//          if(employee.getDepartment().equalsIgnoreCase(department)){
//              result.add(employee);
//          }
//      }
//      return result;
        return employeeRepository.findByDepartmentIgnoreCase(department);
    }

}
