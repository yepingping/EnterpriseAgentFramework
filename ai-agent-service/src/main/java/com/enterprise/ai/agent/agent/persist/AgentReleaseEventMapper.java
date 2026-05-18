package com.enterprise.ai.agent.agent.persist;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface AgentReleaseEventMapper extends BaseMapper<AgentReleaseEventEntity> {

    default List<AgentReleaseEventEntity> listByAgent(String agentId, int limit) {
        return selectList(Wrappers.<AgentReleaseEventEntity>lambdaQuery()
                .eq(AgentReleaseEventEntity::getAgentId, agentId)
                .orderByDesc(AgentReleaseEventEntity::getId)
                .last("limit " + Math.max(1, Math.min(limit, 200))));
    }
}
