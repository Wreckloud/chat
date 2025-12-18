package com.wreckloud.wolfchat.group.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.group.domain.entity.WfGroup;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 群组Mapper接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Mapper
public interface WfGroupMapper extends BaseMapper<WfGroup> {
}

