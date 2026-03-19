package com.wreckloud.wolfchat.notice.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticePageVO;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticeUnreadSummaryVO;
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
    private static final long MERGE_WINDOW_MINUTES = 5L;
    private static final String PAGE_ACHIEVEMENT = "/pages/achievement/achievement";
    private static final String PAGE_FOLLOW = "/pages/follow/follow";
    private static final String PAGE_THREAD_DETAIL_PREFIX = "/pages/post-detail/post-detail?threadId=";
    private static final String PAGE_CHAT_DETAIL_PREFIX = "/pages/chat-detail/chat-detail?conversationId=";
    private static final String PAGE_LOBBY = "/pages/lobby/lobby";

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

    public UserNoticeUnreadSummaryVO countUnreadSummary(Long userId) {
        UserNoticeUnreadSummaryVO summaryVO = wfUserNoticeMapper.selectUnreadSummaryByUserId(userId);
        if (summaryVO == null) {
            summaryVO = new UserNoticeUnreadSummaryVO();
        }
        summaryVO.setTotalUnread(normalizeNonNegative(summaryVO.getTotalUnread()));
        summaryVO.setAchievementUnread(normalizeNonNegative(summaryVO.getAchievementUnread()));
        summaryVO.setFollowUnread(normalizeNonNegative(summaryVO.getFollowUnread()));
        summaryVO.setInteractionUnread(normalizeNonNegative(summaryVO.getInteractionUnread()));
        return summaryVO;
    }

    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long noticeId) {
        WfUserNotice notice = wfUserNoticeMapper.selectById(noticeId);
        if (notice == null || !userId.equals(notice.getUserId())) {
            throw new BaseException(ErrorCode.NOTICE_NOT_FOUND);
        }
        if (Boolean.TRUE.equals(notice.getReadFlag())) {
            return;
        }

        LambdaUpdateWrapper<WfUserNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserNotice::getId, noticeId)
                .eq(WfUserNotice::getUserId, userId)
                .eq(WfUserNotice::getReadFlag, false)
                .set(WfUserNotice::getReadFlag, true)
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
                .eq(WfUserNotice::getReadFlag, false)
                .set(WfUserNotice::getReadFlag, true)
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
        saveNoticeQuietly(userId, NoticeType.ACHIEVEMENT_UNLOCK, content, achievementId);
    }

    public void notifyFollowReceived(Long userId) {
        saveNoticeQuietly(userId, NoticeType.FOLLOW_RECEIVED, "你收到新的关注", null);
    }

    public void notifyThreadLiked(Long threadAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(threadAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(threadAuthorId, NoticeType.THREAD_LIKED, "你的主题收到新的点赞", threadId);
    }

    public void notifyThreadReplied(Long threadAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(threadAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(threadAuthorId, NoticeType.THREAD_REPLIED, "你的主题收到新的回复", threadId);
    }

    public void notifyReplyLiked(Long replyAuthorId, Long threadId, Long operatorUserId) {
        if (isSelfNotice(replyAuthorId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(replyAuthorId, NoticeType.REPLY_LIKED, "你的回复收到新的点赞", threadId);
    }

    public void notifyChatMessageReplied(Long targetUserId, Long conversationId, Long operatorUserId) {
        if (isSelfNotice(targetUserId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(targetUserId, NoticeType.CHAT_MESSAGE_REPLIED, "你的消息收到新的回复", conversationId);
    }

    public void notifyLobbyMessageReplied(Long targetUserId, Long operatorUserId) {
        if (isSelfNotice(targetUserId, operatorUserId)) {
            return;
        }
        saveNoticeQuietly(targetUserId, NoticeType.LOBBY_MESSAGE_REPLIED, "你的大厅消息收到新的回复", null);
    }

    private void saveNoticeQuietly(Long userId, NoticeType noticeType, String content, Long bizId) {
        if (userId == null || noticeType == null || !StringUtils.hasText(content)) {
            return;
        }
        try {
            LocalDateTime now = LocalDateTime.now();
            if (tryMergeRecentUnreadNotice(userId, noticeType, content, bizId, now)) {
                return;
            }

            WfUserNotice notice = new WfUserNotice();
            notice.setUserId(userId);
            notice.setNoticeType(noticeType);
            notice.setContent(content);
            notice.setBizType(noticeType.getBizType());
            notice.setBizId(bizId);
            notice.setReadFlag(false);
            int insertRows = wfUserNoticeMapper.insert(notice);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (Exception ex) {
            log.warn("写入通知失败: userId={}, type={}, bizType={}, bizId={}",
                    userId, noticeType, noticeType.getBizType(), bizId, ex);
        }
    }

    private boolean tryMergeRecentUnreadNotice(
            Long userId,
            NoticeType noticeType,
            String content,
            Long bizId,
            LocalDateTime now
    ) {
        WfUserNotice recentNotice = findRecentUnreadNotice(userId, noticeType, bizId);
        if (recentNotice == null || recentNotice.getCreateTime() == null) {
            return false;
        }

        LocalDateTime mergeThreshold = now.minusMinutes(MERGE_WINDOW_MINUTES);
        if (recentNotice.getCreateTime().isBefore(mergeThreshold)) {
            return false;
        }

        LambdaUpdateWrapper<WfUserNotice> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserNotice::getId, recentNotice.getId())
                .eq(WfUserNotice::getReadFlag, false)
                .set(WfUserNotice::getContent, content)
                .set(WfUserNotice::getCreateTime, now)
                .set(WfUserNotice::getReadTime, null);
        int updateRows = wfUserNoticeMapper.update(null, updateWrapper);
        return updateRows == 1;
    }

    private WfUserNotice findRecentUnreadNotice(Long userId, NoticeType noticeType, Long bizId) {
        LambdaQueryWrapper<WfUserNotice> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserNotice::getUserId, userId)
                .eq(WfUserNotice::getNoticeType, noticeType)
                .eq(WfUserNotice::getReadFlag, false);
        if (bizId == null) {
            queryWrapper.isNull(WfUserNotice::getBizId);
        } else {
            queryWrapper.eq(WfUserNotice::getBizId, bizId);
        }
        queryWrapper.orderByDesc(WfUserNotice::getCreateTime)
                .orderByDesc(WfUserNotice::getId)
                .last("LIMIT 1");
        return wfUserNoticeMapper.selectOne(queryWrapper);
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
        vo.setTypeLabel(notice.getNoticeType() == null ? "通知" : notice.getNoticeType().getLabel());
        vo.setContent(notice.getContent());
        vo.setBizType(notice.getBizType());
        vo.setBizId(notice.getBizId());
        String actionUrl = buildActionUrl(notice);
        vo.setActionUrl(actionUrl);
        vo.setNavigable(StringUtils.hasText(actionUrl));
        vo.setRead(Boolean.TRUE.equals(notice.getReadFlag()));
        vo.setReadTime(notice.getReadTime());
        vo.setCreateTime(notice.getCreateTime());
        return vo;
    }

    private String buildActionUrl(WfUserNotice notice) {
        if (notice == null || notice.getNoticeType() == null) {
            return "";
        }
        switch (notice.getNoticeType()) {
            case ACHIEVEMENT_UNLOCK:
                return PAGE_ACHIEVEMENT;
            case FOLLOW_RECEIVED:
                return PAGE_FOLLOW;
            case THREAD_LIKED:
            case THREAD_REPLIED:
            case REPLY_LIKED:
                if (notice.getBizId() == null || notice.getBizId() <= 0L) {
                    return "";
                }
                return PAGE_THREAD_DETAIL_PREFIX + notice.getBizId();
            case CHAT_MESSAGE_REPLIED:
                if (notice.getBizId() == null || notice.getBizId() <= 0L) {
                    return "";
                }
                return PAGE_CHAT_DETAIL_PREFIX + notice.getBizId();
            case LOBBY_MESSAGE_REPLIED:
                return PAGE_LOBBY;
            default:
                return "";
        }
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

    private long normalizeNonNegative(Long value) {
        if (value == null || value < 0L) {
            return 0L;
        }
        return value;
    }
}
