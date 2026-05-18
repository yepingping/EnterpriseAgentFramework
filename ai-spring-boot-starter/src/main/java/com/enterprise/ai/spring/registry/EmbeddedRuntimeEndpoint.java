package com.enterprise.ai.spring.registry;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/eaf/runtime/embedded")
public class EmbeddedRuntimeEndpoint {

    private final EmbeddedRuntimeService embeddedRuntimeService;

    public EmbeddedRuntimeEndpoint(EmbeddedRuntimeService embeddedRuntimeService) {
        this.embeddedRuntimeService = embeddedRuntimeService;
    }

    @PostMapping("/execute")
    public EmbeddedRuntimeResult execute(@RequestBody EmbeddedRuntimeRequest request) {
        return embeddedRuntimeService.execute(request);
    }
}
