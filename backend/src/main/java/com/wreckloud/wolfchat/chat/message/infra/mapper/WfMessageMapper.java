package com.wreckloud.wolfchat.chat.message.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.chat.message.domain.entity.WfMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 消息 Mapper
 * @Author Wreckloud
 * @Date 2026-01-29
 */
@Mapper
public interface WfMessageMapper extends BaseMapper<WfMessage> {
}

