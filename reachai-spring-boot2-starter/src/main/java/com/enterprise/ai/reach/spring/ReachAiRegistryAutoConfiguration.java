package com.enterprise.ai.reach.spring;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@EnableConfigurationProperties(ReachAiRegistryProperties.class)
public class ReachAiRegistryAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ReachCapabilityBeanScanner reachCapabilityBeanScanner(ApplicationContext applicationContext,
                                                                 ReachAiRegistryProperties properties) {
        return new ReachCapabilityBeanScanner(applicationContext, properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReachAiRegistryTransport reachAiRegistryTransport() {
        return new ReachAiHttpRegistryTransport();
    }

    @Bean
    @ConditionalOnMissingBean
    public ReachAiRegistryClient reachAiRegistryClient(ReachAiRegistryProperties properties,
                                                       ReachCapabilityBeanScanner capabilityBeanScanner,
                                                       ReachAiRegistryTransport transport) {
        return new ReachAiRegistryClient(properties, capabilityBeanScanner, transport);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReachCapabilityInvoker reachCapabilityInvoker(ApplicationContext applicationContext) {
        return new ReachCapabilityInvoker(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean
    public ReachCapabilityInvocationVerifier reachCapabilityInvocationVerifier(ReachAiRegistryProperties properties) {
        return new ReachCapabilityInvocationVerifier(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.RestController")
    public ReachCapabilityEndpoint reachCapabilityEndpoint(ReachCapabilityInvoker invoker,
                                                           ReachCapabilityInvocationVerifier invocationVerifier,
                                                           List<ReachAiSecurityContextBridge> securityContextBridges) {
        return new ReachCapabilityEndpoint(invoker, invocationVerifier, securityContextBridges);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnClass(name = "org.springframework.web.bind.annotation.ControllerAdvice")
    public ReachAiInvocationExceptionHandler reachAiInvocationExceptionHandler() {
        return new ReachAiInvocationExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "reachAiRegistryStartupRunner")
    public ApplicationRunner reachAiRegistryStartupRunner(ReachAiRegistryProperties properties,
                                                          ReachAiRegistryClient registryClient) {
        return args -> {
            if (properties.getRegistry().isEnabled()) {
                registryClient.registerAndSync();
            }
        };
    }
}
