package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
 * @Description 号码池Mapper
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public interface WfNoPoolMapper extends BaseMapper<WfNoPool> {

    /**
     * 乐观锁更新：分配号码
     * 使用 UPDATE ... WHERE 条件，通过影响行数判断是否成功
     *
     * @param wfNo   号码
     * @param userId 用户ID
     * @return 更新的行数，1表示成功，0表示已被占用
     */
    @Update("UPDATE wf_no_pool " +
            "SET status = 1, user_id = #{userId}, update_time = NOW() " +
            "WHERE wf_no = #{wfNo} AND status = 0")
    int allocateNumber(@Param("wfNo") Long wfNo, @Param("userId") Long userId);
}

