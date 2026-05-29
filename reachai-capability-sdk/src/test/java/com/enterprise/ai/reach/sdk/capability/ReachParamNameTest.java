package com.enterprise.ai.reach.sdk.capability;

import com.enterprise.ai.reach.sdk.annotation.ReachCapability;
import com.enterprise.ai.reach.sdk.annotation.ReachParam;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReachParamNameTest {

    @Test
    void explicitReachParamNameWinsOverCompilerParameterName() {
        List<ReachCapabilityDescriptor> descriptors = ReachCapabilityScanner.scanClasses(ContractApi.class);

        assertEquals("contractNo", descriptors.get(0).getParameters().get(0).getName());
    }

    static class ContractApi {
        @ReachCapability(name = "contract.query")
        public String query(@ReachParam(name = "contractNo", description = "Contract number") String contractNo) {
            return contractNo;
        }
    }
}
