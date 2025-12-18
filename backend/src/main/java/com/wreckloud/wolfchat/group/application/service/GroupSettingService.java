package com.wreckloud.wolfchat.group.application.service;

import com.wreckloud.wolfchat.group.api.vo.GroupNoticeVO;

import java.util.List;

/**
 * @Description 群设置服务接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
public interface GroupSettingService {

    /**
     * 发布群公告
     *
     * @param groupId 群组ID
     * @param title 公告标题
     * @param content 公告内容
     * @param isPinned 是否置顶
     * @param publisherId 发布者用户ID
     * @return 群公告信息
     */
    GroupNoticeVO publishNotice(Long groupId, String title, String content, Boolean isPinned, Long publisherId);

    /**
     * 查询群公告列表
     *
     * @param groupId 群组ID
     * @param userId 用户ID（用于校验权限）
     * @return 群公告列表
     */
    List<GroupNoticeVO> getNotices(Long groupId, Long userId);

    /**
     * 删除群公告
     *
     * @param groupId 群组ID
     * @param noticeId 公告ID
     * @param userId 用户ID（需要是群主或管理员）
     */
    void deleteNotice(Long groupId, Long noticeId, Long userId);

    /**
     * 全员禁言/解除全员禁言
     *
     * @param groupId 群组ID
     * @param isMuted true=全员禁言，false=解除全员禁言
     * @param userId 用户ID（需要是群主）
     */
    void muteAll(Long groupId, Boolean isMuted, Long userId);

    /**
     * 禁言/解禁单个成员
     *
     * @param groupId 群组ID
     * @param targetUserId 目标用户ID
     * @param isMuted true=禁言，false=解禁
     * @param muteDuration 禁言时长（秒），0表示永久禁言，null表示取消禁言
     * @param userId 用户ID（需要是群主或管理员）
     */
    void muteMember(Long groupId, Long targetUserId, Boolean isMuted, Long muteDuration, Long userId);

    /**
     * 转让群主
     *
     * @param groupId 群组ID
     * @param newOwnerId 新群主用户ID
     * @param currentOwnerId 当前群主用户ID
     */
    void transferOwner(Long groupId, Long newOwnerId, Long currentOwnerId);
}

