package com.example.leads;

import java.util.LinkedHashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns invalid input into 400 with an RFC 9457 body.
 * The response says WHAT is wrong in general terms, never echoes the input or internals.
 * Spring Boot 2.7 has no built-in problem+json support, so this class does it.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler({
        MethodArgumentNotValidException.class,   // @Valid body failed
        ConstraintViolationException.class,      // @Pattern on a path variable failed
        HttpMessageNotReadableException.class    // body is not valid JSON
    })
    public ResponseEntity<Map<String, Object>> badRequest(Exception ex, HttpServletRequest req) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("type", "about:blank");
        body.put("title", "Bad Request");
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("detail", "The request is not valid");
        body.put("instance", req.getRequestURI());
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
