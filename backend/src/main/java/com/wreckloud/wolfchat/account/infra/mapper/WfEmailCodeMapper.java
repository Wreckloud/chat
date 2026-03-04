package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfEmailCode;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 邮箱验证码 Mapper
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Mapper
public interface WfEmailCodeMapper extends BaseMapper<WfEmailCode> {
}
