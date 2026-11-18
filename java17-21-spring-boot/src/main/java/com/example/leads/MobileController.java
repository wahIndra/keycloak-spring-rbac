package com.example.leads;

import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /mobile/me needs MOBILE_ACCESS. Shows who is calling and which roles Spring sees.
 * Data for the current user is always looked up by "sub", never by an id sent by the client.
 */
@RestController
@RequestMapping("/mobile")
public class MobileController {

    public record Me(String subject, String username, List<String> roles) { }

    @GetMapping("/me")
    public Me me(JwtAuthenticationToken auth) {
        List<String> roles = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .sorted()
                .toList();
        return new Me(auth.getName(), auth.getToken().getClaimAsString("preferred_username"), roles);
    }
}
