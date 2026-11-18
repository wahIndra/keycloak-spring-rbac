package com.example.leads;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/b2b/partners/{partnerId}/leads")
public class PartnerLeadsController {

    public record LeadRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Pattern(regexp = "^\\+?[0-9]{6,20}$") String phone,
            @NotBlank @Size(max = 50) String product) { }

    public record LeadResponse(String id, String status, String partnerId, String submittedBy) { }

    // Two checks, both required:
    //  1. RBAC: the caller has the role B2B_LEADS_SUBMIT
    //  2. Ownership: partnerId in the URL equals partner_id in the token (set by Keycloak, not by the caller)
    @PostMapping
    @PreAuthorize("hasRole('B2B_LEADS_SUBMIT') and #partnerId == authentication.token.claims['partner_id']")
    public LeadResponse submit(@PathVariable @Pattern(regexp = "^[A-Z0-9]{3,20}$") String partnerId,
                        @Valid @RequestBody LeadRequest request,
                        @AuthenticationPrincipal Jwt jwt) {
        // "azp" = the client that called us; store it for auditing
        return new LeadResponse(UUID.randomUUID().toString(), "RECEIVED", partnerId, jwt.getClaimAsString("azp"));
    }
}
