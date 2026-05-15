package com.enterprise.ai.agent.agentscope;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.agent.AgentDefinitionService;
import com.enterprise.ai.agent.model.AgentResult;
import com.enterprise.ai.agent.runtime.AgentRuntimeAdapter;
import com.enterprise.ai.agent.runtime.AgentRuntimeRequest;
import com.enterprise.ai.agent.runtime.AgentRuntimeResult;
import com.enterprise.ai.agent.runtime.AgentRuntimeSelector;
import com.enterprise.ai.agent.service.IntentService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentRouterTest {

    @Test
    void mapsRuntimeStepsToAgentResultAndKeepsMetadataSteps() {
        IntentService intentService = mock(IntentService.class);
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeSelector selector = mock(AgentRuntimeSelector.class);
        AgentRuntimeAdapter adapter = mock(AgentRuntimeAdapter.class);
        AgentRouter router = new AgentRouter(intentService, definitionService, selector);

        when(selector.select(any(AgentRuntimeRequest.class))).thenReturn(adapter);
        when(adapter.execute(any(AgentRuntimeRequest.class))).thenReturn(AgentRuntimeResult.builder()
                .success(true)
                .answer("done")
                .runtimeType(AgentRuntimeAdapter.DEFAULT_RUNTIME_TYPE)
                .traceId("trace-1")
                .agentName("Demo Agent")
                .steps(List.of("意图识别: GENERAL_CHAT", "Runtime: AGENTSCOPE"))
                .metadata(Map.of("elapsedMs", 12L))
                .build());

        AgentResult result = router.executeByDefinition(AgentDefinition.builder()
                        .name("Demo Agent")
                        .keySlug("demo-agent")
                        .intentType("GENERAL_CHAT")
                        .runtimeType(AgentRuntimeAdapter.DEFAULT_RUNTIME_TYPE)
                        .build(),
                "session-1",
                "user-1",
                "hello");

        assertEquals("done", result.getAnswer());
        assertEquals(2, result.getSteps().size());
        assertEquals("Step 1", result.getSteps().get(0).getName());
        assertEquals("意图识别: GENERAL_CHAT", result.getSteps().get(0).getDetail());
        assertEquals("Runtime: AGENTSCOPE", result.getSteps().get(1).getDetail());

        assertNotNull(result.getMetadata());
        assertEquals("trace-1", result.getMetadata().get("traceId"));
        assertEquals(AgentRuntimeAdapter.DEFAULT_RUNTIME_TYPE, result.getMetadata().get("runtimeType"));
        assertEquals("Demo Agent", result.getMetadata().get("agentName"));
        assertEquals(List.of("意图识别: GENERAL_CHAT", "Runtime: AGENTSCOPE"), result.getMetadata().get("steps"));
    }
}
