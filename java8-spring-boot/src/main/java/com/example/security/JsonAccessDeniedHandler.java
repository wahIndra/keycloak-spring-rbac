package com.example.security;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 403: the token is valid, but a role is missing or the data belongs to someone else. */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex)
            throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String principal = auth != null ? auth.getName() : "anonymous";
        String client = auth instanceof JwtAuthenticationToken
                ? ((JwtAuthenticationToken) auth).getToken().getClaimAsString("azp")
                : null;
        // Audit trail: who (sub), through which client (azp), tried what. Never log the token itself.
        log.warn("403 method={} path={} principal={} client={} authorities={}", req.getMethod(),
                SecurityErrorWriter.log(req.getRequestURI()), principal, client,
                auth != null ? auth.getAuthorities() : "[]");
        res.setHeader("WWW-Authenticate", "Bearer error=\"insufficient_scope\"");
        SecurityErrorWriter.write(res, 403, "Forbidden",
                "You don't have permission for this resource", req.getRequestURI());
    }
}
