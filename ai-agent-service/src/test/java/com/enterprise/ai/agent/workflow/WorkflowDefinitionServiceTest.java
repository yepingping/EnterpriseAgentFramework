package com.enterprise.ai.agent.workflow;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowDefinitionServiceTest {

    @Test
    void createFillsDefaultsAndPersistsGraphSpec() {
        WorkflowDefinitionMapper mapper = mock(WorkflowDefinitionMapper.class);
        when(mapper.insert(any(WorkflowDefinitionEntity.class))).thenReturn(1);
        WorkflowDefinitionService service = newService(mapper, mock(WorkflowVersionMapper.class), mock(AgentWorkflowBindingService.class));

        WorkflowDefinitionEntity created = service.create(workflow("page-search", "{\"nodes\":[]}"));

        assertEquals("CHAT", created.getWorkflowType());
        assertEquals("LANGGRAPH4J", created.getRuntimeType());
        assertEquals("DRAFT", created.getStatus());
        assertEquals("MANUAL", created.getManagedBy());
        assertEquals(12, created.getId().length());

        ArgumentCaptor<WorkflowDefinitionEntity> captor = ArgumentCaptor.forClass(WorkflowDefinitionEntity.class);
        verify(mapper).insert(captor.capture());
        assertTrue(captor.getValue().getGraphSpecJson().contains("nodes"));
    }

    @Test
    void deleteDraftWithoutBindingsRemovesWorkflowAndVersions() {
        WorkflowDefinitionMapper mapper = mock(WorkflowDefinitionMapper.class);
        WorkflowVersionMapper versionMapper = mock(WorkflowVersionMapper.class);
        AgentWorkflowBindingService bindingService = mock(AgentWorkflowBindingService.class);
        WorkflowDefinitionService service = newService(mapper, versionMapper, bindingService);

        WorkflowDefinitionEntity draft = workflow("metro-qa", "{\"nodes\":[]}");
        draft.setId("wf-1");
        when(mapper.selectById("wf-1")).thenReturn(draft);
        when(bindingService.listByWorkflowId("wf-1")).thenReturn(List.of());
        when(mapper.deleteById("wf-1")).thenReturn(1);

        service.delete("wf-1");

        verify(versionMapper).delete(any());
        verify(mapper).deleteById("wf-1");
    }

    @Test
    void deletePublishedWorkflowIsRejected() {
        WorkflowDefinitionMapper mapper = mock(WorkflowDefinitionMapper.class);
        WorkflowDefinitionService service = newService(mapper, mock(WorkflowVersionMapper.class), mock(AgentWorkflowBindingService.class));

        WorkflowDefinitionEntity published = workflow("metro-qa", "{\"nodes\":[]}");
        published.setId("wf-1");
        published.setStatus("ACTIVE");
        when(mapper.selectById("wf-1")).thenReturn(published);

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.delete("wf-1"));

        assertEquals("仅草稿状态的 Workflow 可删除", error.getMessage());
        verify(mapper, never()).deleteById("wf-1");
    }

    @Test
    void deleteBoundDraftIsRejected() {
        WorkflowDefinitionMapper mapper = mock(WorkflowDefinitionMapper.class);
        AgentWorkflowBindingService bindingService = mock(AgentWorkflowBindingService.class);
        WorkflowDefinitionService service = newService(mapper, mock(WorkflowVersionMapper.class), bindingService);

        WorkflowDefinitionEntity draft = workflow("page-assistant", "{\"nodes\":[]}");
        draft.setId("wf-2");
        when(mapper.selectById("wf-2")).thenReturn(draft);
        when(bindingService.listByWorkflowId("wf-2")).thenReturn(List.of(new AgentWorkflowBindingEntity()));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> service.delete("wf-2"));

        assertEquals("该 Workflow 仍被 Agent 绑定，请先解除绑定后再删除", error.getMessage());
        verify(mapper, never()).deleteById("wf-2");
    }

    @Test
    void isDeletableRequiresDraftAndNoBindings() {
        WorkflowDefinitionMapper mapper = mock(WorkflowDefinitionMapper.class);
        AgentWorkflowBindingService bindingService = mock(AgentWorkflowBindingService.class);
        WorkflowDefinitionService service = newService(mapper, mock(WorkflowVersionMapper.class), bindingService);

        WorkflowDefinitionEntity draft = workflow("metro-qa", "{\"nodes\":[]}");
        draft.setId("wf-3");
        when(mapper.selectById("wf-3")).thenReturn(draft);
        when(bindingService.listByWorkflowId("wf-3")).thenReturn(List.of());

        assertTrue(service.isDeletable("wf-3"));

        draft.setStatus("ACTIVE");
        assertFalse(service.isDeletable("wf-3"));

        draft.setStatus("DRAFT");
        when(bindingService.listByWorkflowId("wf-3")).thenReturn(List.of(new AgentWorkflowBindingEntity()));
        assertFalse(service.isDeletable("wf-3"));
    }

    private WorkflowDefinitionService newService(WorkflowDefinitionMapper mapper,
                                                   WorkflowVersionMapper versionMapper,
                                                   AgentWorkflowBindingService bindingService) {
        return new WorkflowDefinitionService(mapper, versionMapper, bindingService);
    }

    private WorkflowDefinitionEntity workflow(String keySlug, String graphSpecJson) {
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setProjectId(101L);
        entity.setProjectCode("demo");
        entity.setKeySlug(keySlug);
        entity.setName("Page Search");
        entity.setGraphSpecJson(graphSpecJson);
        return entity;
    }
}
