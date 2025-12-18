package com.wreckloud.wolfchat.group.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.group.domain.entity.WfGroupMember;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 群成员Mapper接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Mapper
public interface WfGroupMemberMapper extends BaseMapper<WfGroupMember> {
}

