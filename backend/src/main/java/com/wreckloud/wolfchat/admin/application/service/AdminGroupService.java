package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.dto.GroupDismissDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminGroupVO;

/**
 * @Description 管理员-群组管理服务接口
 * @Author Wreckloud
 * @Date 2024-12-18
 */
public interface AdminGroupService {
    
    /**
     * 分页查询群组列表
     * @param current 当前页
     * @param size 每页大小
     * @param keyword 搜索关键词（群名称）
     * @param status 状态筛选
     * @return 群组列表
     */
    Page<AdminGroupVO> getGroupList(Long current, Long size, String keyword, Integer status);
    
    /**
     * 查询群组详情
     * @param groupId 群组ID
     * @return 群组详情
     */
    AdminGroupVO getGroupDetail(Long groupId);
    
    /**
     * 强制解散群组
     * @param dto 解散请求
     * @param adminId 操作管理员ID
     */
    void dismissGroup(GroupDismissDTO dto, Long adminId);
}

