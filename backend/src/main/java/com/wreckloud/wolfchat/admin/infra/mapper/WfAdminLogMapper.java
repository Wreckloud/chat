package com.wreckloud.wolfchat.admin.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.admin.domain.entity.WfAdminLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 管理员操作日志Mapper
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Mapper
public interface WfAdminLogMapper extends BaseMapper<WfAdminLog> {
}

