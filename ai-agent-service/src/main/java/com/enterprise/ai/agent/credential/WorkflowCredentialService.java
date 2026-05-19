package com.enterprise.ai.agent.credential;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowCredentialService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final WorkflowCredentialMapper mapper;
    private final WorkflowCredentialCipher cipher;
    private final ObjectMapper objectMapper;

    public List<WorkflowCredentialResponse> list(Long projectId, String projectCode) {
        return mapper.selectList(Wrappers.<WorkflowCredentialEntity>lambdaQuery()
                        .eq(WorkflowCredentialEntity::getStatus, "ACTIVE")
                        .and(projectId != null || StringUtils.hasText(projectCode), q -> {
                            if (projectId != null) {
                                q.eq(WorkflowCredentialEntity::getProjectId, projectId).or()
                                        .eq(WorkflowCredentialEntity::getScope, "GLOBAL");
                            }
                            if (StringUtils.hasText(projectCode)) {
                                q.eq(WorkflowCredentialEntity::getProjectCode, projectCode.trim()).or()
                                        .eq(WorkflowCredentialEntity::getScope, "GLOBAL");
                            }
                        })
                        .orderByAsc(WorkflowCredentialEntity::getName))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public WorkflowCredentialResponse create(WorkflowCredentialRequest request) {
        WorkflowCredentialEntity entity = new WorkflowCredentialEntity();
        fill(entity, request, false);
        if (!StringUtils.hasText(entity.getCredentialRef())) {
            entity.setCredentialRef("cred_" + UUID.randomUUID().toString().replace("-", ""));
        }
        LocalDateTime now = LocalDateTime.now();
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return toResponse(entity);
    }

    public WorkflowCredentialResponse update(Long id, WorkflowCredentialRequest request) {
        WorkflowCredentialEntity entity = mapper.selectById(id);
        if (entity == null) {
            throw new IllegalArgumentException("Credential not found: " + id);
        }
        fill(entity, request, true);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
        return toResponse(entity);
    }

    public void delete(Long id) {
        WorkflowCredentialEntity entity = mapper.selectById(id);
        if (entity == null) {
            return;
        }
        entity.setStatus("DISABLED");
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    public Optional<WorkflowCredentialRuntime> resolve(String credentialRef, Long projectId, String projectCode) {
        if (!StringUtils.hasText(credentialRef)) {
            return Optional.empty();
        }
        WorkflowCredentialEntity entity = mapper.selectOne(Wrappers.<WorkflowCredentialEntity>lambdaQuery()
                .eq(WorkflowCredentialEntity::getCredentialRef, credentialRef.trim())
                .eq(WorkflowCredentialEntity::getStatus, "ACTIVE")
                .and(projectId != null || StringUtils.hasText(projectCode), q -> {
                    q.eq(WorkflowCredentialEntity::getScope, "GLOBAL");
                    if (projectId != null) {
                        q.or().eq(WorkflowCredentialEntity::getProjectId, projectId);
                    }
                    if (StringUtils.hasText(projectCode)) {
                        q.or().eq(WorkflowCredentialEntity::getProjectCode, projectCode.trim());
                    }
                })
                .last("LIMIT 1"));
        if (entity == null) {
            return Optional.empty();
        }
        return Optional.of(WorkflowCredentialRuntime.builder()
                .credentialRef(entity.getCredentialRef())
                .name(entity.getName())
                .type(entity.getType())
                .secret(readSecret(entity))
                .build());
    }

    public Map<String, Object> readSecretForTest(Long id) {
        WorkflowCredentialEntity entity = mapper.selectById(id);
        return entity == null ? Map.of() : readSecret(entity);
    }

    private void fill(WorkflowCredentialEntity entity, WorkflowCredentialRequest request, boolean preserveEmptySecret) {
        if (request == null) {
            throw new IllegalArgumentException("Credential request is required");
        }
        if (StringUtils.hasText(request.getCredentialRef())) {
            entity.setCredentialRef(request.getCredentialRef().trim());
        }
        entity.setName(required(request.getName(), "name"));
        entity.setType(required(request.getType(), "type").toUpperCase());
        entity.setProjectId(request.getProjectId());
        entity.setProjectCode(trimToNull(request.getProjectCode()));
        entity.setScope(StringUtils.hasText(request.getScope()) ? request.getScope().trim().toUpperCase() : "PROJECT");
        entity.setStatus(StringUtils.hasText(request.getStatus()) ? request.getStatus().trim().toUpperCase() : "ACTIVE");
        if (!preserveEmptySecret || request.getSecret() != null) {
            entity.setSecretJson(cipher.encrypt(toJson(request.getSecret() == null ? Map.of() : request.getSecret())));
        }
    }

    private WorkflowCredentialResponse toResponse(WorkflowCredentialEntity entity) {
        return WorkflowCredentialResponse.builder()
                .id(entity.getId())
                .credentialRef(entity.getCredentialRef())
                .name(entity.getName())
                .type(entity.getType())
                .projectId(entity.getProjectId())
                .projectCode(entity.getProjectCode())
                .scope(entity.getScope())
                .status(entity.getStatus())
                .secretPreview(mask(readSecret(entity)))
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private Map<String, Object> readSecret(WorkflowCredentialEntity entity) {
        try {
            String json = cipher.decrypt(entity.getSecretJson());
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception e) {
            return Map.of();
        }
    }

    private Map<String, Object> mask(Map<String, Object> secret) {
        Map<String, Object> masked = new LinkedHashMap<>();
        secret.forEach((key, value) -> masked.put(key, maskValue(value)));
        return masked;
    }

    private Object maskValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            map.forEach((key, child) -> masked.put(String.valueOf(key), maskValue(child)));
            return masked;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(this::maskValue).toList();
        }
        String text = value == null ? "" : String.valueOf(value);
        if (text.isBlank()) {
            return "";
        }
        return text.length() <= 4 ? "****" : text.substring(0, 2) + "****" + text.substring(text.length() - 2);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid credential secret", e);
        }
    }

    private String required(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("Credential " + field + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
