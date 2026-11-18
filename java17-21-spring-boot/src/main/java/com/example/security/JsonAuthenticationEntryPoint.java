package com.example.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/** 401: no token, expired token, or invalid token. */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException ex)
            throws IOException {
        boolean hasToken = req.getHeader("Authorization") != null;
        if (hasToken) {
            // The reason stays in the server log. The client only gets a generic message.
            log.info("401 method={} path={} reason={}", req.getMethod(),
                    SecurityErrorWriter.log(req.getRequestURI()), SecurityErrorWriter.log(ex.getMessage()));
        }
        // RFC 6750: no error code when the request had no token at all
        res.setHeader("WWW-Authenticate", hasToken ? "Bearer error=\"invalid_token\"" : "Bearer");
        SecurityErrorWriter.write(res, 401, "Unauthorized",
                "Missing, expired, or invalid access token", req.getRequestURI());
    }
}
