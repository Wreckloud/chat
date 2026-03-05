package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUserProfile;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 用户资料 Mapper
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Mapper
public interface WfUserProfileMapper extends BaseMapper<WfUserProfile> {
}
