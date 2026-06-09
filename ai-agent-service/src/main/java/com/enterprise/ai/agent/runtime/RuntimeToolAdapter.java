package com.enterprise.ai.agent.runtime;

import com.enterprise.ai.runtime.contract.AiTool;

public interface RuntimeToolAdapter<T> {

    String runtimeType();

    T adapt(AiTool tool, ToolRuntimeContext context);
}
