package com.enterprise.ai.agent.skill;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public record SubAgentSpec(
        String systemPrompt,
        List<String> toolWhitelist,
        String modelInstanceId,
        int maxSteps,
        boolean useMultiAgentModel
) {

    public SubAgentSpec {
        toolWhitelist = toolWhitelist == null ? List.of() : List.copyOf(toolWhitelist);
    }
}
