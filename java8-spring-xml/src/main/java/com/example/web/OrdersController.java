package com.example.web;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Access rules are in applicationContext-security.xml:
 *   GET  /health       public
 *   POST /order        needs ORDERS_WRITE
 *   POST /payment      needs PAYMENTS_WRITE
 *   GET  /status/{id}  needs ORDERS_WRITE or PAYMENTS_WRITE
 *   anything else      denied
 *
 * All input is validated. Invalid input gets 400 (see ApiExceptionHandler).
 */
@Validated
@RestController
public class OrdersController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        return result("status", "UP");
    }

    @PostMapping("/order")
    public Map<String, Object> createOrder(@Valid @RequestBody OrderRequest request) {
        Map<String, Object> response = result("orderId", UUID.randomUUID().toString());
        response.put("item", request.getItem());
        return response;
    }

    @PostMapping("/payment")
    public Map<String, Object> createPayment(@Valid @RequestBody PaymentRequest request) {
        Map<String, Object> response = result("paymentId", UUID.randomUUID().toString());
        response.put("amount", request.getAmount());
        return response;
    }

    @GetMapping("/status/{id}")
    public Map<String, Object> status(@PathVariable @Pattern(regexp = "^[A-Za-z0-9-]{1,40}$") String id) {
        Map<String, Object> response = result("id", id);
        response.put("status", "PROCESSING");
        return response;
    }

    /** Not listed in the security rules on purpose: always 401 or 403. */
    @GetMapping("/internal/ping")
    public Map<String, Object> ping() {
        return result("message", "pong");
    }

    private static Map<String, Object> result(String key, Object value) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(key, value);
        return map;
    }
}
