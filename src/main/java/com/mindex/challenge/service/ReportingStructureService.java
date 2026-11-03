package com.mindex.challenge.service;

import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.data.ReportingStructure;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

@Service
public class ReportingStructureService {
    private final EmployeeRepository employees;

    public ReportingStructureService(EmployeeRepository employees) {
        this.employees = employees;
    }

    public ReportingStructure read(String employeeId) {
        Employee root = employees.findByEmployeeId(employeeId);
        if (root == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + employeeId);
        }
        Set<String> visited = new HashSet<>();
        visited.add(employeeId);
        ArrayDeque<Employee> pending = new ArrayDeque<>();
        pending.add(root);
        while (!pending.isEmpty()) {
            Employee employee = pending.remove();
            if (employee.getDirectReports() == null) continue;
            for (Employee reference : employee.getDirectReports()) {
                if (reference == null || reference.getEmployeeId() == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid direct report reference");
                }
                String reportId = reference.getEmployeeId();
                if (!visited.add(reportId)) continue;
                Employee report = employees.findByEmployeeId(reportId);
                if (report == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Direct report not found: " + reportId);
                }
                pending.add(report);
            }
        }
        return new ReportingStructure(root, visited.size() - 1);
    }
}
