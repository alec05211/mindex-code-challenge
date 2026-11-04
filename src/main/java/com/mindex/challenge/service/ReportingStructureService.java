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
        Set<String> visited = new HashSet<>(); // hashset to keep track of who in the initial employee chain has been evaluated. if already evaluated they shouldnt be reconsidered for subsequent count increment
        visited.add(employeeId);
        ArrayDeque<Employee> pending = new ArrayDeque<>(); // pending holds active evaluation candidates.
        pending.add(root);
        while (!pending.isEmpty()) {
            Employee employee = pending.remove(); // gets first person in queue to evaluate if they have sub reports to visit and count.
            if (employee.getDirectReports() == null) continue;
            for (Employee reference : employee.getDirectReports()) {
                if (reference == null || reference.getEmployeeId() == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Invalid direct report reference");
                }
                String reportId = reference.getEmployeeId();
                if (!visited.add(reportId)) continue; // if current employeeId has already been visited (and thus added to total report count) then continue and re loop with next pending candidate.
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
