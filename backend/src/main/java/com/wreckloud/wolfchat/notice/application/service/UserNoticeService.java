package com.wreckloud.wolfchat.notice.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticePageVO;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticeVO;
import com.wreckloud.wolfchat.notice.domain.entity.WfUserNotice;
import com.wreckloud.wolfchat.notice.domain.enums.NoticeType;
import com.wreckloud.wolfchat.notice.infra.mapper.WfUserNoticeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 用户通知服务
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserNoticeService {
    private static final long DEFAULT_PAGE = 1L;
    private static final long DEFAULT_SIZE = 20L;
    private static final long MAX_SIZE = 50L;
    private static final String BIZ_TYPE_ACHIEVEMENT = "ACHIEVEMENT";
    private static final String BIZ_TYPE_FOLLOW = "FOLLOW";
    private static final String BIZ_TYPE_THREAD = "THREAD";

    private final WfUserNoticeMapper wfUserNoticeMapper;

    public UserNoticePageVO listNotices(Long userId, long page, long size) {
        long current = normalizePage(page);
        long pageSize = normalizeSize(size);

        LambdaQueryWrapper<WfUserNotice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserNotice::getUserId, userId)
                .orderByDesc(WfUserNotice::getCreateTime)
                .orderByDesc(WfUserNotice::getId);

        Page<WfUserNotice> noticePage = wfUserNoticeMapper.selectPage(new Page<>(current, pageSize), queryWrapper);
        List<WfUserNotice> records = noticePage.getRecords();

        UserNoticePageVO pageVO = new UserNoticePageVO();
        pageVO.setList(toNoticeVOList(records));
        pageVO.setTotal(noticePage.getTotal());
        pageVO.setPage(current);
        pageVO.setSize(pageSize);
        return pageVO;
    }

    public long countUnread(Long userId) {
        Long count = wfUserNoticeMapper.selectUnreadCountByUserId(userId);
        if (count == null || count < 0) {
            return 0L;
        }
        return count;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long noticeId) {
        WfUserNotice notice = wfUserNoticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            throw new BaseException(ErrorCode.NOTICE_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(notice.getRead())) {
            return;
        }

        LambdaUpdateWrapper<WfUserNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserNotice::getId, noticeId)
                .eq(WfUserNotice::getUserId, userId)
                .eq(WfUserNotice::getRead, false)
                .set(WfUserNotice::getRead, true)
                .set(WfUserNotice::getReadTime, LocalDateTime.now());
        int updateRows = wfUserNoticeMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        LambdaUpdateWrapper<WfUserNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserNotice::getUserId, userId)
                .eq(WfUserNotice::getRead, false)
                .set(WfUserNotice::getRead, true)
                .set(WfUserNotice::getReadTime, LocalDateTime.now());
        wfUserNoticeMapper.update(null, updateWrapper);
    }

    public void notifyAchievementUnlocked(Long userId, Long achievementId, String achievementName, String titleName) {
        String safeAchievementName = normalizeText(achievementName);
        if (!StringUtils.hasText(safeAchievementName)) {
            safeAchievementName = "新成就";
        }
        String safeTitleName = normalizeText(titleName);
        String content = StringUtils.hasText(safeTitleName)
                ? "你解锁了成就「" + safeAchievementName + "」，获得头衔「" + safeTitleName + "」"
                : "你解锁了成就「" + safeAchievementName + "」";
        saveNoticeQuietly(userId, NoticeType.ACHIEVEMENT_UNLOCK, content, BIZ_TYPE_ACHIEVEMENT, achievementId);
    }

    public void notifyFollowReceived(Long userId) {
        saveNoticeQuietly(userId, NoticeType.FOLLOW_RECEIVED, "你收到新的关注", BIZ_TYPE_FOLLOW, null);
    }

    public void notifyThreadLiked(Long threadAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(threadAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(threadAuthorId, NoticeType.THREAD_LIKED, "你的主题收到新的点赞", BIZ_TYPE_THREAD, threadId);
    }

    public void notifyThreadReplied(Long threadAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(threadAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(threadAuthorId, NoticeType.THREAD_REPLIED, "你的主题收到新的回复", BIZ_TYPE_THREAD, threadId);
    }

    public void notifyReplyLiked(Long replyAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(replyAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(replyAuthorId, NoticeType.REPLY_LIKED, "你的回复收到新的点赞", BIZ_TYPE_THREAD, threadId);
    }

    private void saveNoticeQuietly(Long userId, NoticeType noticeType, String content, String bizType, Long bizId) {
        if (userId == null || noticeType == null || !StringUtils.hasText(content)) {
            return;
        }
        try {
            WfUserNotice notice = new WfUserNotice();
            notice.setUserId(userId);
            notice.setNoticeType(noticeType);
            notice.setContent(content);
            notice.setBizType(bizType);
            notice.setBizId(bizId);
            notice.setRead(false);
            int insertRows = wfUserNoticeMapper.insert(notice);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (Exception ex) {
            log.warn("写入通知失败: userId={}, type={}, bizType={}, bizId={}",
                    userId, noticeType, bizType, bizId, ex);
        }
    }

    private List<UserNoticeVO> toNoticeVOList(List<WfUserNotice> records) {
        if (records == null || records.isEmpty()) {
            return Collections.emptyList();
        }
        return records.stream().map(this::toNoticeVO).collect(Collectors.toList());
    }

    private UserNoticeVO toNoticeVO(WfUserNotice notice) {
        UserNoticeVO vo = new UserNoticeVO();
        vo.setNoticeId(notice.getId());
        vo.setNoticeType(notice.getNoticeType());
        vo.setContent(notice.getContent());
        vo.setBizType(notice.getBizType());
        vo.setBizId(notice.getBizId());
        vo.setRead(Boolean.TRUE.equals(notice.getRead()));
        vo.setReadTime(notice.getReadTime());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    private boolean isSelfNotice(Long targetUserId, Long operatorUserId) {
        return targetUserId != null && targetUserId.equals(operatorUserId);
    }

    private String normalizeText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim();
    }

    private long normalizePage(long page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private long normalizeSize(long size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }
}
