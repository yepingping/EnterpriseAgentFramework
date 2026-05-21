package com.enterprise.ai.agent.studio;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.workflow-draft")
public class WorkflowDraftProperties {

    /**
     * Cursor SDK draft provider switch. Keep enabled by default so local rule-based
     * draft generation is available even before a real Cursor workspace is wired.
     */
    private boolean cursorEnabled = true;

    private String cursorUnavailableReason = "Cursor SDK 流程草稿生成器未启用";
}
