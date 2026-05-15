package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.agent.tool.log.ToolExecutionContext;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ToolRuntimeContext {

    private String runtimeType;

    private String toolName;

    private ToolExecutionContext executionContext;
}
