package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumModerationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 论坛版务日志 Mapper
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Mapper
public interface WfForumModerationLogMapper extends BaseMapper<WfForumModerationLog> {
}
