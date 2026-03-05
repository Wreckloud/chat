package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUserBanRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 用户封禁记录 Mapper
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Mapper
public interface WfUserBanRecordMapper extends BaseMapper<WfUserBanRecord> {
}
