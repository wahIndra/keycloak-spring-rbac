package com.example.leads;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This endpoint is NOT listed in SecurityConfig on purpose.
 * It shows "deny by default": every call gets 401 (no token) or 403 (with a token).
 */
@RestController
public class InternalController {

    @GetMapping("/internal/ping")
    public String ping() {
        return "pong";
    }
}
