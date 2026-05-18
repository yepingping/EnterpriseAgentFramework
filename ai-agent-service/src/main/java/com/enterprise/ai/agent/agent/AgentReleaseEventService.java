package com.enterprise.ai.agent.agent;

import com.enterprise.ai.agent.agent.persist.AgentReleaseEventEntity;
import com.enterprise.ai.agent.agent.persist.AgentReleaseEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class AgentReleaseEventService {

    private final AgentReleaseEventMapper mapper;
    private final ObjectMapper objectMapper;

    public AgentReleaseEventService(AgentReleaseEventMapper mapper, ObjectMapper objectMapper) {
        this.mapper = mapper;
        this.objectMapper = objectMapper;
    }

    public AgentReleaseEventEntity record(String agentId,
                                          Long versionId,
                                          String version,
                                          String action,
                                          String decision,
                                          Integer rolloutPercent,
                                          String operator,
                                          String summary,
                                          AgentReleaseValidationResult validation,
                                          Map<String, ?> metadata) {
        try {
            AgentReleaseEventEntity entity = new AgentReleaseEventEntity();
            entity.setAgentId(agentId);
            entity.setVersionId(versionId);
            entity.setVersion(version);
            entity.setAction(action);
            entity.setDecision(decision);
            entity.setRolloutPercent(rolloutPercent);
            entity.setOperator(operator);
            entity.setSummary(summary);
            entity.setValidationJson(toJson(validation));
            entity.setMetadataJson(toJson(metadata));
            entity.setCreatedAt(LocalDateTime.now());
            mapper.insert(entity);
            return entity;
        } catch (Exception ex) {
            log.warn("[AgentReleaseEvent] 写入失败: agentId={}, version={}, action={}, err={}",
                    agentId, version, action, ex.toString());
            return null;
        }
    }

    public List<AgentReleaseEventEntity> listByAgent(String agentId, Integer limit) {
        return mapper.listByAgent(agentId, limit == null ? 100 : limit);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
