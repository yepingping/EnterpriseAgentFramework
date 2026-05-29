package com.enterprise.ai.reach.spring;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/reachai/capabilities")
public class ReachCapabilityEndpoint {

    private final ReachCapabilityInvoker invoker;

    public ReachCapabilityEndpoint(ReachCapabilityInvoker invoker) {
        this.invoker = invoker;
    }

    @PostMapping("/{capabilityName}/invoke")
    public Object invoke(@PathVariable String capabilityName,
                         @RequestBody(required = false) Map<String, Object> arguments) {
        return invoker.invoke(capabilityName, arguments);
    }
}
