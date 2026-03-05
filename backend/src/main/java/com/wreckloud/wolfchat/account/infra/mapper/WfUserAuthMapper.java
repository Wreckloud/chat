package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUserAuth;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 用户认证 Mapper
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Mapper
public interface WfUserAuthMapper extends BaseMapper<WfUserAuth> {
}
