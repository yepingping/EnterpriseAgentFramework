package com.enterprise.ai.agent.assist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("eaf_ai_access_session")
public class AiAccessSessionEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long projectId;

    private String projectCode;

    private String toolName;

    private String scenario;

    private String targetPageKey;

    private String targetRoute;

    private String metadataJson;

    private String status;

    private Integer totalSteps;

    private Integer completedSteps;

    private Integer failedSteps;

    private String lastMessage;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
