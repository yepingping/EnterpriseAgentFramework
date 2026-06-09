package com.enterprise.ai.reach.spring;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.LinkedHashMap;
import java.util.Map;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ReachAiInvocationExceptionHandler {

    @ExceptionHandler(ReachAiInvocationUnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorized(ReachAiInvocationUnauthorizedException ex) {
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("success", false);
        body.put("code", String.valueOf(HttpStatus.UNAUTHORIZED.value()));
        body.put("message", ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
}
