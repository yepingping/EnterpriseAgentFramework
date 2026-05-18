package com.enterprise.ai.agent.agent.persist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Agent production lifecycle audit event.
 */
@Data
@TableName("agent_release_event")
public class AgentReleaseEventEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String agentId;

    private Long versionId;

    private String version;

    /** VALIDATE / PUBLISH / ROLLBACK. */
    private String action;

    /** PASSED / BLOCKED / COMPLETED. */
    private String decision;

    private Integer rolloutPercent;

    private String operator;

    private String summary;

    private String validationJson;

    private String metadataJson;

    private LocalDateTime createdAt;
}
