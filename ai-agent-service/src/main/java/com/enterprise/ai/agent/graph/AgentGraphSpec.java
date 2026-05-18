package com.enterprise.ai.agent.graph;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;

import java.util.List;
import java.util.Map;

/**
 * Platform-level graph contract for workflow-style Agent execution.
 * <p>
 * This is intentionally independent from frontend canvas layout and from any
 * concrete runtime such as LangGraph4j. Runtime adapters compile this contract
 * into their native representation.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AgentGraphSpec {

    private String code;

    private String name;

    @Builder.Default
    private String mode = "WORKFLOW";

    private String runtimeHint;

    private Map<String, Object> inputSchema;

    private Map<String, Object> stateSchema;

    @Singular
    private List<Node> nodes;

    @Singular
    private List<Edge> edges;

    private String entry;

    @Singular("finishNode")
    private List<String> finish;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Node {
        private String id;

        private String type;

        private String name;

        private CapabilityRef ref;

        private Map<String, Object> config;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Edge {
        private String from;

        private String to;

        private String condition;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CapabilityRef {
        private String kind;

        private String name;

        private String qualifiedName;

        private Long definitionId;

        private String projectCode;
    }
}
