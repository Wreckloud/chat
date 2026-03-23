package com.wreckloud.wolfchat.ai.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.ai.domain.entity.WfAiSessionSummary;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 会话摘要 Mapper。
 */
@Mapper
public interface WfAiSessionSummaryMapper extends BaseMapper<WfAiSessionSummary> {
}
