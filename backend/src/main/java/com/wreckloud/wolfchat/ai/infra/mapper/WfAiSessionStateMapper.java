package com.wreckloud.wolfchat.ai.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiSessionState;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话状态 Mapper。
 */
@Mapper
public interface WfAiSessionStateMapper extends BaseMapper<WfAiSessionState> {
}
