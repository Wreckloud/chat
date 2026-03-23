package com.wreckloud.wolfchat.ai.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiUserMemoryFact;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 用户长期记忆 Mapper。
 */
@Mapper
public interface WfAiUserMemoryFactMapper extends BaseMapper<WfAiUserMemoryFact> {
}
