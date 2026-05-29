package com.enterprise.ai.reach.spring;

import com.enterprise.ai.reach.sdk.annotation.ReachCapability;
import com.enterprise.ai.reach.sdk.annotation.ReachParam;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReachCapabilityEndpointTest {

    @Test
    void delegatesInvokeRequestToCapabilityInvoker() {
        ReachCapabilityEndpoint endpoint = new ReachCapabilityEndpoint(
                new ReachCapabilityInvoker(new Object[]{new ContractCapability()}));
        Map<String, Object> arguments = new LinkedHashMap<String, Object>();
        arguments.put("contractNo", "HT-002");

        Object result = endpoint.invoke("contract.query", arguments);

        assertEquals("contract:HT-002", result);
    }

    static class ContractCapability {
        @ReachCapability(name = "contract.query")
        public String query(@ReachParam(name = "contractNo", required = true) String contractNo) {
            return "contract:" + contractNo;
        }
    }
}
