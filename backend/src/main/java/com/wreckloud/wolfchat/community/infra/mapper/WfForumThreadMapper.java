package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 论坛主题 Mapper
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Mapper
public interface WfForumThreadMapper extends BaseMapper<WfForumThread> {
}
