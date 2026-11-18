package com.example.leads;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/b2b/partners/{partnerId}/leads")
public class PartnerLeadsController {

    // Two checks, both required:
    //  1. RBAC: the caller has the role B2B_LEADS_SUBMIT
    //  2. Ownership: partnerId in the URL equals partner_id in the token (set by Keycloak, not by the caller)
    // The class and method must be public, or the security proxy may not apply @PreAuthorize.
    @PostMapping
    @PreAuthorize("hasRole('B2B_LEADS_SUBMIT') and #partnerId == authentication.token.claims['partner_id']")
    public Map<String, Object> submit(@PathVariable @Pattern(regexp = "^[A-Z0-9]{3,20}$") String partnerId,
                                      @Valid @RequestBody PartnerLeadRequest request,
                                      @AuthenticationPrincipal Jwt jwt) {
        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("id", UUID.randomUUID().toString());
        response.put("status", "RECEIVED");
        response.put("partnerId", partnerId);
        // "azp" = the client that called us; store it for auditing
        response.put("submittedBy", jwt.getClaimAsString("azp"));
        return response;
    }
}
