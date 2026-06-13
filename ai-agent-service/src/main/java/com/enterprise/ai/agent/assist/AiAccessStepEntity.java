package com.enterprise.ai.agent.assist;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("eaf_ai_access_step")
public class AiAccessStepEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String sessionId;

    private Long projectId;

    private String stepKey;

    private String title;

    private String status;

    private String message;

    private String filesJson;

    private String evidenceJson;

    private String reportedBy;

    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    private LocalDateTime updatedAt;
}
