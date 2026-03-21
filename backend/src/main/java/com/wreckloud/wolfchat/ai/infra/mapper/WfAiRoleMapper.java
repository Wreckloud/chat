package com.wreckloud.wolfchat.ai.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 角色配置 Mapper。
 */
@Mapper
public interface WfAiRoleMapper extends BaseMapper<WfAiRole> {
}

