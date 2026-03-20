package com.wreckloud.wolfchat.follow.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.follow.domain.entity.WfUserBlock;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户拉黑关系 Mapper
 */
@Mapper
public interface WfUserBlockMapper extends BaseMapper<WfUserBlock> {
}

