package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfEmailCode;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;

/**
 * @Description 邮箱验证码 Mapper
 * @Author Wreckloud
 * @Date 2026-03-04
 */
@Mapper
public interface WfEmailCodeMapper extends BaseMapper<WfEmailCode> {
    /**
     * 批量删除早于截止时间的过期验证码
     *
     * @param deadline 过期截止时间
     * @param limit    单批删除数量上限
     * @return 删除条数
     */
    @Delete("DELETE FROM wf_email_code WHERE expire_time < #{deadline} LIMIT #{limit}")
    int deleteExpiredBefore(@Param("deadline") LocalDateTime deadline, @Param("limit") int limit);
}
