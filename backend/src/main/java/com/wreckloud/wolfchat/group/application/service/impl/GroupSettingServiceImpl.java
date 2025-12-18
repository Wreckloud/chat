package com.wreckloud.wolfchat.group.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.group.api.vo.GroupNoticeVO;
import com.wreckloud.wolfchat.group.application.service.GroupSettingService;
import com.wreckloud.wolfchat.group.domain.entity.WfGroup;
import com.wreckloud.wolfchat.group.domain.entity.WfGroupMember;
import com.wreckloud.wolfchat.group.domain.entity.WfGroupNotice;
import com.wreckloud.wolfchat.group.domain.enums.GroupMemberRole;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMapper;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMemberMapper;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupNoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 群设置服务实现类
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupSettingServiceImpl implements GroupSettingService {

    private final WfGroupMapper groupMapper;
    private final WfGroupMemberMapper groupMemberMapper;
    private final WfGroupNoticeMapper groupNoticeMapper;
    private final WfUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupNoticeVO publishNotice(Long groupId, String title, String content, Boolean isPinned, Long publisherId) {
        log.info("发布群公告，群组ID: {}, 发布者ID: {}", groupId, publisherId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验权限：只有群主和管理员可以发布公告
        WfGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, publisherId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (member == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
        if (!GroupMemberRole.isAdminOrOwner(member.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 如果是置顶公告，先取消其他置顶公告
        if (Boolean.TRUE.equals(isPinned)) {
            groupNoticeMapper.update(null,
                    new LambdaUpdateWrapper<WfGroupNotice>()
                            .eq(WfGroupNotice::getGroupId, groupId)
                            .eq(WfGroupNotice::getIsPinned, true)
                            .set(WfGroupNotice::getIsPinned, false)
            );
        }

        // 4. 创建公告
        WfGroupNotice notice = new WfGroupNotice();
        notice.setGroupId(groupId);
        notice.setPublisherId(publisherId);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setIsPinned(isPinned);
        groupNoticeMapper.insert(notice);

        // 5. 查询发布者信息
        WfUser publisher = userMapper.selectById(publisherId);

        // 6. 转换为VO
        GroupNoticeVO vo = new GroupNoticeVO();
        vo.setNoticeId(notice.getId());
        vo.setGroupId(groupId);
        vo.setTitle(title);
        vo.setContent(content);
        vo.setPublisherId(publisherId);
        vo.setPublisherName(publisher != null ? publisher.getUsername() : null);
        vo.setIsPinned(isPinned);
        vo.setPublishTime(notice.getPublishTime());

        log.info("群公告发布成功，公告ID: {}", notice.getId());
        return vo;
    }

    @Override
    public List<GroupNoticeVO> getNotices(Long groupId, Long userId) {
        log.info("查询群公告列表，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验用户是否在群内
        WfGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (member == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 查询所有公告
        List<WfGroupNotice> notices = groupNoticeMapper.selectList(
                new LambdaQueryWrapper<WfGroupNotice>()
                        .eq(WfGroupNotice::getGroupId, groupId)
                        .eq(WfGroupNotice::getIsDeleted, false)
                        .orderByDesc(WfGroupNotice::getIsPinned)
                        .orderByDesc(WfGroupNotice::getPublishTime)
        );

        if (notices.isEmpty()) {
            return new ArrayList<>();
        }

        // 4. 查询所有发布者信息
        List<Long> publisherIds = notices.stream().map(WfGroupNotice::getPublisherId).distinct().collect(Collectors.toList());
        List<WfUser> publishers = userMapper.selectBatchIds(publisherIds);
        Map<Long, WfUser> publisherMap = publishers.stream().collect(Collectors.toMap(WfUser::getId, p -> p));

        // 5. 转换为VO
        return notices.stream().map(notice -> {
            WfUser publisher = publisherMap.get(notice.getPublisherId());
            GroupNoticeVO vo = new GroupNoticeVO();
            vo.setNoticeId(notice.getId());
            vo.setGroupId(groupId);
            vo.setTitle(notice.getTitle());
            vo.setContent(notice.getContent());
            vo.setPublisherId(notice.getPublisherId());
            vo.setPublisherName(publisher != null ? publisher.getUsername() : null);
            vo.setIsPinned(notice.getIsPinned());
            vo.setPublishTime(notice.getPublishTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteNotice(Long groupId, Long noticeId, Long userId) {
        log.info("删除群公告，群组ID: {}, 公告ID: {}, 用户ID: {}", groupId, noticeId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验权限：只有群主和管理员可以删除公告
        WfGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (member == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
        if (!GroupMemberRole.isAdminOrOwner(member.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 查询公告
        WfGroupNotice notice = groupNoticeMapper.selectById(noticeId);
        if (notice == null || notice.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (!notice.getGroupId().equals(groupId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 4. 删除公告（软删除）
        notice.setIsDeleted(true);
        groupNoticeMapper.updateById(notice);

        log.info("群公告删除成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteAll(Long groupId, Boolean isMuted, Long userId) {
        log.info("全员禁言设置，群组ID: {}, 是否禁言: {}, 用户ID: {}", groupId, isMuted, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验权限：只有群主可以设置全员禁言
        if (!group.getOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 设置全员禁言
        group.setIsAllMuted(isMuted);
        groupMapper.updateById(group);

        log.info("全员禁言设置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void muteMember(Long groupId, Long targetUserId, Boolean isMuted, Long muteDuration, Long userId) {
        log.info("禁言成员，群组ID: {}, 目标用户ID: {}, 是否禁言: {}, 禁言时长: {}秒, 操作人ID: {}", 
                groupId, targetUserId, isMuted, muteDuration, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 查询操作人角色
        WfGroupMember operatorMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (operatorMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }
        if (!GroupMemberRole.isAdminOrOwner(operatorMember.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 查询目标用户
        WfGroupMember targetMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, targetUserId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (targetMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 4. 不能禁言群主
        if (GroupMemberRole.OWNER.getCode().equals(targetMember.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 5. 管理员不能禁言其他管理员
        if (GroupMemberRole.ADMIN.getCode().equals(operatorMember.getRole()) &&
            GroupMemberRole.ADMIN.getCode().equals(targetMember.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 6. 设置禁言
        targetMember.setIsMuted(isMuted);
        if (Boolean.TRUE.equals(isMuted) && muteDuration != null && muteDuration > 0) {
            // 设置禁言结束时间
            targetMember.setMuteUntil(LocalDateTime.now().plusSeconds(muteDuration));
        } else {
            // 永久禁言或解除禁言
            targetMember.setMuteUntil(null);
        }
        groupMemberMapper.updateById(targetMember);

        log.info("成员禁言设置成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferOwner(Long groupId, Long newOwnerId, Long currentOwnerId) {
        log.info("转让群主，群组ID: {}, 新群主ID: {}, 当前群主ID: {}", groupId, newOwnerId, currentOwnerId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验当前用户是否是群主
        if (!group.getOwnerId().equals(currentOwnerId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 查询新群主是否在群内
        WfGroupMember newOwnerMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, newOwnerId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (newOwnerMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 4. 查询当前群主成员记录
        WfGroupMember currentOwnerMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, currentOwnerId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );

        // 5. 更新群组的群主
        group.setOwnerId(newOwnerId);
        groupMapper.updateById(group);

        // 6. 更新成员角色：新群主设置为OWNER，原群主设置为MEMBER
        newOwnerMember.setRole(GroupMemberRole.OWNER.getCode());
        groupMemberMapper.updateById(newOwnerMember);

        if (currentOwnerMember != null) {
            currentOwnerMember.setRole(GroupMemberRole.MEMBER.getCode());
            groupMemberMapper.updateById(currentOwnerMember);
        }

        log.info("群主转让成功");
    }
}

