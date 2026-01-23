package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 帖子 Mapper
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Mapper
public interface WfPostMapper extends BaseMapper<WfPost> {
}
