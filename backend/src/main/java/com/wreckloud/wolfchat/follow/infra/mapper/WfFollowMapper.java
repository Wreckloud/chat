package com.wreckloud.wolfchat.follow.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 关注关系 Mapper
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Mapper
public interface WfFollowMapper extends BaseMapper<WfFollow> {
}
