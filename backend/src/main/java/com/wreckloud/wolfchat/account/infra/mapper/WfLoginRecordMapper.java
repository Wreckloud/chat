package com.wreckloud.wolfchat.account.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.account.domain.entity.WfLoginRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 登录记录 Mapper
 * @Author Wreckloud
 * @Date 2026-03-05
 */
@Mapper
public interface WfLoginRecordMapper extends BaseMapper<WfLoginRecord> {
}
