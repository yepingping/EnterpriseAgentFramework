package com.enterprise.ai.agent.workflow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AgentWorkflowResolverTest {

    @Test
    void resolvePrefersExactPageActionBindingOverPageAndDefaultBindings() {
        AgentWorkflowBindingMapper mapper = mock(AgentWorkflowBindingMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                binding("default-flow", "DEFAULT", null, null, null, null, 100),
                binding("page-flow", "PAGE", "team.list", null, null, null, 20),
                binding("action-flow", "ACTION", "team.list", "archive", null, null, 1)
        ));
        AgentWorkflowResolver resolver = new AgentWorkflowResolver(mapper);

        Optional<AgentWorkflowBindingEntity> resolved = resolver.resolve(new AgentWorkflowResolveRequest(
                "entry-agent",
                "demo",
                "team.list",
                "/teams",
                "archive",
                "GENERAL_CHAT"));

        assertTrue(resolved.isPresent());
        assertEquals("action-flow", resolved.get().getWorkflowId());
    }

    @Test
    void resolveUsesPriorityInsideSameMatchRank() {
        AgentWorkflowBindingMapper mapper = mock(AgentWorkflowBindingMapper.class);
        when(mapper.selectList(any())).thenReturn(List.of(
                binding("low", "PAGE", "team.list", null, null, null, 1),
                binding("high", "PAGE", "team.list", null, null, null, 9)
        ));
        AgentWorkflowResolver resolver = new AgentWorkflowResolver(mapper);

        Optional<AgentWorkflowBindingEntity> resolved = resolver.resolve(new AgentWorkflowResolveRequest(
                "entry-agent",
                "demo",
                "team.list",
                "/teams",
                null,
                null));

        assertTrue(resolved.isPresent());
        assertEquals("high", resolved.get().getWorkflowId());
    }

    @Test
    void resolveUsesLatestBindingWhenRankAndPriorityTie() {
        AgentWorkflowBindingMapper mapper = mock(AgentWorkflowBindingMapper.class);
        AgentWorkflowBindingEntity oldBinding = binding("old-unpublished", "PAGE", "team.list", null, null, "/teams", 100);
        oldBinding.setId(1L);
        oldBinding.setUpdatedAt(LocalDateTime.parse("2026-06-15T10:00:00"));
        AgentWorkflowBindingEntity latestBinding = binding("latest-published", "PAGE", "team.list", null, null, "/teams", 100);
        latestBinding.setId(2L);
        latestBinding.setUpdatedAt(LocalDateTime.parse("2026-06-15T11:00:00"));
        when(mapper.selectList(any())).thenReturn(List.of(oldBinding, latestBinding));
        AgentWorkflowResolver resolver = new AgentWorkflowResolver(mapper);

        Optional<AgentWorkflowBindingEntity> resolved = resolver.resolve(new AgentWorkflowResolveRequest(
                "entry-agent",
                "demo",
                "team.list",
                "/teams",
                null,
                null));

        assertTrue(resolved.isPresent());
        assertEquals("latest-published", resolved.get().getWorkflowId());
    }

    private AgentWorkflowBindingEntity binding(String workflowId,
                                               String type,
                                               String pageKey,
                                               String actionKey,
                                               String intentType,
                                               String routePattern,
                                               int priority) {
        AgentWorkflowBindingEntity entity = new AgentWorkflowBindingEntity();
        entity.setAgentId("entry-agent");
        entity.setProjectCode("demo");
        entity.setWorkflowId(workflowId);
        entity.setBindingType(type);
        entity.setPageKey(pageKey);
        entity.setActionKey(actionKey);
        entity.setIntentType(intentType);
        entity.setRoutePattern(routePattern);
        entity.setPriority(priority);
        entity.setEnabled(true);
        return entity;
    }
}
