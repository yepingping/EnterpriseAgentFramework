package com.enterprise.ai.agent.credential;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowCredentialServiceTest {

    @Test
    void createEncryptsSecretAndMasksResponse() {
        WorkflowCredentialMapper mapper = mock(WorkflowCredentialMapper.class);
        WorkflowCredentialService service = new WorkflowCredentialService(
                mapper,
                new WorkflowCredentialCipher("unit-test-secret"),
                new ObjectMapper());

        WorkflowCredentialRequest request = new WorkflowCredentialRequest();
        request.setName("Orders API");
        request.setType("BEARER");
        request.setProjectCode("demo");
        request.setSecret(Map.of("token", "secret-token-123"));

        WorkflowCredentialResponse response = service.create(request);

        ArgumentCaptor<WorkflowCredentialEntity> captor = ArgumentCaptor.forClass(WorkflowCredentialEntity.class);
        verify(mapper).insert(captor.capture());
        WorkflowCredentialEntity entity = captor.getValue();

        assertTrue(entity.getSecretJson().startsWith("aesgcm:"));
        assertFalse(entity.getSecretJson().contains("secret-token-123"));
        assertEquals("BEARER", entity.getType());
        assertEquals("se****23", response.getSecretPreview().get("token"));
    }

    @Test
    void resolveDecryptsRuntimeSecret() {
        WorkflowCredentialCipher cipher = new WorkflowCredentialCipher("unit-test-secret");
        WorkflowCredentialMapper mapper = mock(WorkflowCredentialMapper.class);
        WorkflowCredentialEntity entity = new WorkflowCredentialEntity();
        entity.setCredentialRef("cred_orders");
        entity.setName("Orders API");
        entity.setType("API_KEY_HEADER");
        entity.setStatus("ACTIVE");
        entity.setSecretJson(cipher.encrypt("{\"headerName\":\"X-API-Key\",\"apiKey\":\"abc123\"}"));
        when(mapper.selectOne(any(Wrapper.class))).thenReturn(entity);

        WorkflowCredentialService service = new WorkflowCredentialService(mapper, cipher, new ObjectMapper());

        Optional<WorkflowCredentialRuntime> runtime = service.resolve("cred_orders", null, "demo");

        assertTrue(runtime.isPresent());
        assertEquals("X-API-Key", runtime.get().getSecret().get("headerName"));
        assertEquals("abc123", runtime.get().getSecret().get("apiKey"));
    }
}
