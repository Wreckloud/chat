package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfComment;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 评论 Mapper
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Mapper
public interface WfCommentMapper extends BaseMapper<WfComment> {
}
