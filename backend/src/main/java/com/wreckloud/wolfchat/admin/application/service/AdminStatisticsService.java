package com.wreckloud.wolfchat.admin.application.service;

import com.wreckloud.wolfchat.admin.api.vo.AdminStatisticsVO;

/**
 * @Description 管理员-统计服务接口
 * @Author Wreckloud
 * @Date 2024-12-18
 */
public interface AdminStatisticsService {
    
    /**
     * 获取系统统计数据
     * @return 统计数据
     */
    AdminStatisticsVO getStatistics();
}

