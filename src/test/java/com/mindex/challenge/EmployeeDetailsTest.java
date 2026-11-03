package com.mindex.challenge;

import com.mindex.challenge.dao.CompensationRepository;
import com.mindex.challenge.dao.EmployeeRepository;
import com.mindex.challenge.data.Employee;
import com.mindex.challenge.service.ReportingStructureService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
public class EmployeeDetailsTest {
    private static final String JOHN = "16a596ae-edd3-4847-99fe-c4518e82c86f";
    @Autowired private MockMvc mvc;
    @Autowired private EmployeeRepository employees;
    @Autowired private CompensationRepository compensations;
    @Autowired private MongoTemplate mongoTemplate;

    @Test
    public void reportingStructureCountsAllLevelsAndLeaves() throws Exception {
        mvc.perform(get("/employee/{id}/reporting-structure", JOHN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.employee.firstName").value("John"))
                .andExpect(jsonPath("$.numberOfReports").value(4));
        mvc.perform(get("/employee/b7839309-3348-463b-a7e3-5de1c168beb3/reporting-structure"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.numberOfReports").value(0));
        mvc.perform(get("/employee/missing/reporting-structure")).andExpect(status().isNotFound());
    }

    @Test
    public void distinctReportsAndCyclesAreHandled() {
        EmployeeRepository repository = mock(EmployeeRepository.class);
        Employee root = employee("root");
        Employee child = employee("child");
        Employee leaf = employee("leaf");
        root.setDirectReports(List.of(child, leaf, child));
        child.setDirectReports(List.of(leaf, root));
        when(repository.findByEmployeeId("root")).thenReturn(root);
        when(repository.findByEmployeeId("child")).thenReturn(child);
        when(repository.findByEmployeeId("leaf")).thenReturn(leaf);
        assertEquals(2, new ReportingStructureService(repository).read("root").numberOfReports());
        verify(repository, times(1)).findByEmployeeId("leaf");
    }

    @Test
    public void compensationPersistsAndRejectsDuplicateAndInvalidRequests() throws Exception {
        Employee employee = employees.insert(employee(UUID.randomUUID().toString()));
        String url = "/employee/" + employee.getEmployeeId() + "/compensation";
        try {
            mvc.perform(get(url)).andExpect(status().isNotFound());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salary\":-1,\"effectiveDate\":\"2026-11-03\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"employeeId\":\"other\",\"salary\":10,\"effectiveDate\":\"2026-11-03\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salary\":10,\"effectiveDate\":\"invalid\"}"))
                    .andExpect(status().isBadRequest());
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salary\":123456.78,\"effectiveDate\":\"2026-11-03\"}"))
                    .andExpect(status().isCreated()).andExpect(jsonPath("$.employeeId").value(employee.getEmployeeId()));
            assertEquals(0, new BigDecimal("123456.78").compareTo(
                    compensations.findById(employee.getEmployeeId()).orElseThrow().getSalary()));
            mvc.perform(get(url)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.salary").value(123456.78))
                    .andExpect(jsonPath("$.effectiveDate").value("2026-11-03"));
            mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salary\":10,\"effectiveDate\":\"2026-11-03\"}"))
                    .andExpect(status().isConflict());
            mvc.perform(post("/employee/missing/compensation").contentType(MediaType.APPLICATION_JSON)
                    .content("{\"salary\":10,\"effectiveDate\":\"2026-11-03\"}"))
                    .andExpect(status().isNotFound());
        } finally {
            compensations.deleteById(employee.getEmployeeId());
            mongoTemplate.remove(Query.query(Criteria.where("employeeId").is(employee.getEmployeeId())), Employee.class);
        }
    }

    private static Employee employee(String id) {
        Employee employee = new Employee();
        employee.setEmployeeId(id);
        return employee;
    }
}
