package com.example.leads;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET  /leads/{id}  needs LEADS_READ or LEADS_OPERATOR  (see SecurityConfig)
 * POST /leads       needs LEADS_WRITE or LEADS_OPERATOR
 *
 * All input is validated. Invalid input gets 400 (see ApiExceptionHandler).
 */
@Validated
@RestController
@RequestMapping("/leads")
public class LeadsController {

    @GetMapping("/{id}")
    public Map<String, Object> get(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{1,40}$") String id) {
        return lead(id, "Sample lead");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> create(@Valid @RequestBody NewLead request) {
        return lead(UUID.randomUUID().toString(), request.getName());
    }

    private static Map<String, Object> lead(String id, String name) {
        Map<String, Object> lead = new LinkedHashMap<String, Object>();
        lead.put("id", id);
        lead.put("name", name);
        lead.put("status", "NEW");
        return lead;
    }
}
