package com.mindex.challenge.controller;

import com.mindex.challenge.data.Compensation;
import com.mindex.challenge.data.ReportingStructure;
import com.mindex.challenge.service.CompensationService;
import com.mindex.challenge.service.ReportingStructureService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee/{id}")
public class EmployeeDetailsController {
    private final ReportingStructureService reportingStructures;
    private final CompensationService compensations;

    public EmployeeDetailsController(ReportingStructureService reportingStructures, CompensationService compensations) {
        this.reportingStructures = reportingStructures;
        this.compensations = compensations;
    }

    @GetMapping("/reporting-structure")
    public ReportingStructure reportingStructure(@PathVariable String id) {
        return reportingStructures.read(id);
    }

    @PostMapping("/compensation")
    @ResponseStatus(HttpStatus.CREATED)
    public Compensation createCompensation(@PathVariable String id, @RequestBody Compensation compensation) {
        return compensations.create(id, compensation);
    }

    @GetMapping("/compensation")
    public Compensation readCompensation(@PathVariable String id) {
        return compensations.read(id);
    }
}
