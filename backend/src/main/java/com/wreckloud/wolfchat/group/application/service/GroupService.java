package com.wreckloud.wolfchat.group.application.service;

import com.wreckloud.wolfchat.group.api.dto.GroupCreateDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupUpdateDTO;
import com.wreckloud.wolfchat.group.api.vo.GroupDetailVO;
import com.wreckloud.wolfchat.group.api.vo.GroupVO;

import java.util.List;

/**
 * @Description 群组服务接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
public interface GroupService {

    /**
     * 创建群组
     *
     * @param dto 创建群组DTO
     * @param creatorId 创建者用户ID
     * @return 群组信息
     */
    GroupVO createGroup(GroupCreateDTO dto, Long creatorId);

    /**
     * 查询群组详情
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID
     * @return 群组详情
     */
    GroupDetailVO getGroupDetail(Long groupId, Long userId);

    /**
     * 查询我的群组列表
     *
     * @param userId 用户ID
     * @return 群组列表
     */
    List<GroupVO> getMyGroups(Long userId);

    /**
     * 修改群信息
     *
     * @param groupId 群组ID
     * @param dto 修改群信息DTO
     * @param userId 当前用户ID（需要校验权限）
     */
    void updateGroup(Long groupId, GroupUpdateDTO dto, Long userId);

    /**
     * 解散群组
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID（需要校验是群主）
     */
    void disbandGroup(Long groupId, Long userId);
}

