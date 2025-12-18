package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.vo.AdminLogVO;
import com.wreckloud.wolfchat.admin.domain.entity.WfAdminLog;

/**
 * @Description 管理员-操作日志服务接口
 * @Author Wreckloud
 * @Date 2024-12-18
 */
public interface AdminLogService {
    
    /**
     * 记录管理员操作日志
     * @param log 日志实体
     */
    void saveLog(WfAdminLog log);
    
    /**
     * 分页查询操作日志
     * @param current 当前页
     * @param size 每页大小
     * @param adminId 管理员ID筛选
     * @param action 操作类型筛选
     * @return 日志列表
     */
    Page<AdminLogVO> getLogList(Long current, Long size, Long adminId, String action);
}

