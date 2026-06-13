package com.enterprise.ai.agent.agent;

import com.enterprise.ai.agent.agent.persist.AgentDefinitionEntity;
import com.enterprise.ai.agent.agent.persist.AgentDefinitionMapper;
import com.enterprise.ai.agent.graph.GraphSpec;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentDefinitionServiceTest {

    @Test
    void createBootstrapsEmptyLangGraphDefinitionWithStarterNode() {
        AgentDefinitionMapper mapper = mock(AgentDefinitionMapper.class);
        when(mapper.insert(any(AgentDefinitionEntity.class))).thenReturn(1);
        AgentDefinitionService service = new AgentDefinitionService(mapper, null);

        AgentDefinition created = assertDoesNotThrow(() -> service.create(AgentDefinition.builder()
                .name("班组管理页面嵌入智能体")
                .intentType("GENERAL_CHAT")
                .runtimeType("LANGGRAPH4J")
                .graphSpec(GraphSpec.builder()
                        .code("team_page_agent")
                        .name("班组管理页面嵌入智能体")
                        .build())
                .build()));

        GraphSpec graphSpec = created.getGraphSpec();
        assertEquals("LANGGRAPH4J", graphSpec.getRuntimeHint());
        assertEquals("starter_answer", graphSpec.getEntry());
        assertEquals(1, graphSpec.getNodes().size());
        assertEquals("ANSWER", graphSpec.getNodes().get(0).getType());
        assertEquals(2, graphSpec.getEdges().size());
        assertNull(created.getModelInstanceId());
    }

    @Test
    void createAllowsLangGraphPageActionAgentWithoutDefaultModel() {
        AgentDefinitionMapper mapper = mock(AgentDefinitionMapper.class);
        when(mapper.insert(any(AgentDefinitionEntity.class))).thenReturn(1);
        AgentDefinitionService service = new AgentDefinitionService(mapper, null);

        AgentDefinition created = assertDoesNotThrow(() -> service.create(AgentDefinition.builder()
                .name("班组档案页面查询")
                .intentType("QUERY_DATA")
                .runtimeType("LANGGRAPH4J")
                .graphSpec(GraphSpec.builder()
                        .node(GraphSpec.Node.builder()
                                .id("apply_filters")
                                .type("PAGE_ACTION")
                                .config(Map.of("actionKey", "page.search.applyFilters"))
                                .build())
                        .edge(GraphSpec.Edge.builder().from("START").to("apply_filters").condition("always").build())
                        .edge(GraphSpec.Edge.builder().from("apply_filters").to("END").condition("always").build())
                        .entry("apply_filters")
                        .finishNode("apply_filters")
                        .build())
                .build()));

        assertNull(created.getModelInstanceId());
        ArgumentCaptor<AgentDefinitionEntity> captor = ArgumentCaptor.forClass(AgentDefinitionEntity.class);
        verify(mapper).insert(captor.capture());
        assertNull(captor.getValue().getModelInstanceId());
    }

    @Test
    void createStillRequiresDefaultModelForAgentScopeAgent() {
        AgentDefinitionService service = new AgentDefinitionService(mock(AgentDefinitionMapper.class), null);

        assertThrows(IllegalArgumentException.class, () -> service.create(AgentDefinition.builder()
                .name("通用对话")
                .intentType("GENERAL_CHAT")
                .runtimeType("AGENTSCOPE")
                .tools(List.of())
                .build()));
    }
}
