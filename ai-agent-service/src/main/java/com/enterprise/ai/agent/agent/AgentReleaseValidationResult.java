package com.enterprise.ai.agent.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * Validation report used before an Agent version can be published.
 */
public record AgentReleaseValidationResult(
        boolean valid,
        List<Item> errors,
        List<Item> warnings
) {

    public static AgentReleaseValidationResult ok() {
        return new AgentReleaseValidationResult(true, List.of(), List.of());
    }

    public static Builder builder() {
        return new Builder();
    }

    public record Item(
            String code,
            String level,
            String nodeId,
            String message
    ) {
    }

    public static class Builder {
        private final List<Item> errors = new ArrayList<>();
        private final List<Item> warnings = new ArrayList<>();

        public Builder error(String code, String nodeId, String message) {
            errors.add(new Item(code, "ERROR", nodeId, message));
            return this;
        }

        public Builder warn(String code, String nodeId, String message) {
            warnings.add(new Item(code, "WARN", nodeId, message));
            return this;
        }

        public AgentReleaseValidationResult build() {
            return new AgentReleaseValidationResult(errors.isEmpty(), List.copyOf(errors), List.copyOf(warnings));
        }
    }
}
