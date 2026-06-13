package com.enterprise.ai.agent.controller;

import com.enterprise.ai.agent.agent.AgentDefinition;
import com.enterprise.ai.agent.agent.AgentDefinitionService;
import com.enterprise.ai.agent.graph.GraphSpec;
import com.enterprise.ai.agent.runtime.AgentRuntimeRequest;
import com.enterprise.ai.agent.runtime.AgentRuntimeSelector;
import com.enterprise.ai.agent.runtime.AgentRuntimeValidationResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentManageControllerTest {

    @Test
    void runtimeValidationBootstrapsEmptyLangGraphDefinitionBeforeSelectorValidation() {
        AgentRuntimeSelector runtimeSelector = mock(AgentRuntimeSelector.class);
        when(runtimeSelector.validate(any())).thenReturn(AgentRuntimeValidationResult.builder()
                .valid(true)
                .build());
        AgentManageController controller = new AgentManageController(mock(AgentDefinitionService.class), runtimeSelector);

        controller.validateRuntime(AgentDefinition.builder()
                .name("班组管理页面嵌入智能体")
                .intentType("GENERAL_CHAT")
                .runtimeType("LANGGRAPH4J")
                .graphSpec(GraphSpec.builder().build())
                .build());

        ArgumentCaptor<AgentRuntimeRequest> captor = ArgumentCaptor.forClass(AgentRuntimeRequest.class);
        verify(runtimeSelector).validate(captor.capture());
        GraphSpec graphSpec = captor.getValue().getAgentDefinition().getGraphSpec();
        assertEquals("starter_answer", graphSpec.getEntry());
        assertEquals(1, graphSpec.getNodes().size());
        assertEquals("ANSWER", graphSpec.getNodes().get(0).getType());
    }
}
