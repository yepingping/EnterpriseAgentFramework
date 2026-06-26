package com.enterprise.ai.agent.platform.control.controller;

import com.enterprise.ai.agent.aicoding.AiCodingAccessDeniedException;
import com.enterprise.ai.agent.aicoding.AiCodingUnauthorizedException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice(assignableTypes = {
        AiCodingGatewayController.class,
        AiCodingContextCandidateController.class
})
public class AiCodingGatewayControllerAdvice {

    @ExceptionHandler(AiCodingUnauthorizedException.class)
    public ResponseEntity<Map<String, String>> unauthorized(RuntimeException ex) {
        return ResponseEntity.status(401).body(Map.of(
                "message", ex.getMessage() == null ? "aiCodingKey is required" : ex.getMessage()));
    }

    @ExceptionHandler(AiCodingAccessDeniedException.class)
    public ResponseEntity<Map<String, String>> forbidden(RuntimeException ex) {
        return ResponseEntity.status(403).body(Map.of(
                "message", ex.getMessage() == null ? "invalid AI Coding access key" : ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        String message = ex.getMessage() == null ? "invalid request" : ex.getMessage();
        if (message.contains("not found")) {
            return ResponseEntity.status(404).body(Map.of("message", message));
        }
        return ResponseEntity.badRequest().body(Map.of("message", message));
    }
}
