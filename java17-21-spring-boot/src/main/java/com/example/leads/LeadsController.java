package com.example.leads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.http.HttpStatus;
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
 * All input is validated. Invalid input gets 400 with a problem+json body.
 */
@RestController
@RequestMapping("/leads")
public class LeadsController {

    public record Lead(String id, String name, String status) { }

    public record NewLead(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{6,20}$") String phone) { }

    @GetMapping("/{id}")
    public Lead get(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{1,40}$") String id) {
        return new Lead(id, "Sample lead", "NEW");
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Lead create(@Valid @RequestBody NewLead request) {
        return new Lead(UUID.randomUUID().toString(), request.name(), "NEW");
    }
}
