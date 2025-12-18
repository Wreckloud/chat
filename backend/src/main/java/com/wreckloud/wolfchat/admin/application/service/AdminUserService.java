package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.dto.UserStatusUpdateDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminUserVO;

/**
 * @Description 管理员-用户管理服务接口
 * @Author Wreckloud
 * @Date 2024-12-18
 */
public interface AdminUserService {
    
    /**
     * 分页查询用户列表
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 搜索关键词（用户名/手机号/WF号）
     * @param status 状态筛选
     * @return 用户列表
     */
    Page<AdminUserVO> getUserList(Long current, Long size, String keyword, Integer status);
    
    /**
     * 查询用户详情
     * @param userId 用户ID
     * @return 用户详情
     */
    AdminUserVO getUserDetail(Long userId);
    
    /**
     * 更新用户状态（禁用/启用/注销）
     * @param dto 更新请求
     * @param adminId 操作管理员ID
     */
    void updateUserStatus(UserStatusUpdateDTO dto, Long adminId);
}

