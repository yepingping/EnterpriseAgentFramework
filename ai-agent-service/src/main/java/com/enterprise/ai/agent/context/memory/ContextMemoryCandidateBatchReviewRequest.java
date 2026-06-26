package com.enterprise.ai.agent.context.memory;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ContextMemoryCandidateBatchReviewRequest extends ContextMemoryCandidateReviewRequest {

    private List<Long> candidateIds;
}
