package com.enterprise.ai.reach.spring;

import java.io.IOException;
import java.util.Map;

public interface ReachAiRegistryTransport {

    String exchange(String method, String url, Map<String, String> headers, Object body) throws IOException;
}
