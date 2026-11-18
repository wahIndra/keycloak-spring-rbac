package com.example.security;

import java.io.IOException;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Writes an RFC 9457 "problem details" body (application/problem+json).
 * Never includes stack traces, exception messages, or token content.
 */
final class SecurityErrorWriter {

    private SecurityErrorWriter() { }

    static void write(HttpServletResponse res, int status, String title, String detail, String instance)
            throws IOException {
        res.setStatus(status);
        res.setContentType("application/problem+json");
        res.setCharacterEncoding("UTF-8");
        res.setHeader("Cache-Control", "no-store");
        res.getWriter().write("{\"type\":\"about:blank\""
                + ",\"title\":\"" + json(title) + "\""
                + ",\"status\":" + status
                + ",\"detail\":\"" + json(detail) + "\""
                + ",\"instance\":\"" + json(instance) + "\"}");
    }

    /** Escapes a value for a JSON string. */
    static String json(String s) {
        if (s == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toCharArray()) {
            if (c == '"' || c == '\\') {
                out.append('\\').append(c);
            } else if (c < 0x20) {
                out.append(String.format("\\u%04x", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /** Removes line breaks so request data cannot fake extra log lines. */
    static String log(String s) {
        return s == null ? "" : s.replace('\r', '_').replace('\n', '_');
    }
}
