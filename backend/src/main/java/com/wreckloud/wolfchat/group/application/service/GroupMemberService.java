package com.wreckloud.wolfchat.group.application.service;

import com.wreckloud.wolfchat.group.api.vo.GroupMemberVO;

import java.util.List;

/**
 * @Description 群成员服务接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
public interface GroupMemberService {

    /**
     * 邀请成员入群
     *
     * @param groupId 群组ID
     * @param userIds 被邀请用户ID列表
     * @param inviterId 邀请人用户ID
     */
    void inviteMembers(Long groupId, List<Long> userIds, Long inviterId);

    /**
     * 踢出成员
     *
     * @param groupId 群组ID
     * @param targetUserId 被踢出用户ID
     * @param operatorId 操作人用户ID
     */
    void kickMember(Long groupId, Long targetUserId, Long operatorId);

    /**
     * 退出群组
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     */
    void quitGroup(Long groupId, Long userId);

    /**
     * 设置/取消管理员
     *
     * @param groupId 群组ID
     * @param targetUserId 目标用户ID
     * @param isAdmin true=设置为管理员，false=取消管理员
     * @param operatorId 操作人用户ID（需要是群主）
     */
    void setAdmin(Long groupId, Long targetUserId, Boolean isAdmin, Long operatorId);

    /**
     * 查询群成员列表
     *
     * @param groupId 群组ID
     * @param userId 当前用户ID（用于校验权限）
     * @return 群成员列表
     */
    List<GroupMemberVO> getMembers(Long groupId, Long userId);

    /**
     * 修改群昵称
     *
     * @param groupId 群组ID
     * @param userId 用户ID
     * @param nickname 新的群昵称
     */
    void updateNickname(Long groupId, Long userId, String nickname);
}

