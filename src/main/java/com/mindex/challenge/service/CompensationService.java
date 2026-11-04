package com.mindex.challenge.service;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Compensation;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompensationService {
    private final CompensationRepository compensations;
    private final EmployeeRepository employees;

    public CompensationService(CompensationRepository compensations, EmployeeRepository employees) {
        this.compensations = compensations;
        this.employees = employees;
    }

    public Compensation create(String employeeId, Compensation compensation) {
        requireEmployee(employeeId);
        if (compensation.getSalary() == null || compensation.getSalary().signum() < 0 || compensation.getEffectiveDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A nonnegative salary and effectiveDate are required");
        }
        if (compensation.getEmployeeId() != null && !employeeId.equals(compensation.getEmployeeId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "employeeId must match the URL");
        }
        compensation.setEmployeeId(employeeId);
        try {
            return compensations.insert(compensation);
        } catch (DuplicateKeyException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Compensation already exists", ex);
        }
    }

    public Compensation read(String employeeId) {
        requireEmployee(employeeId);
        return compensations.findById(employeeId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Compensation not found"));
    }

    private void requireEmployee(String employeeId) {
        if (employees.findByEmployeeId(employeeId) == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Employee not found: " + employeeId);
        }
    }
}
