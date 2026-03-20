package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * 社区内容写操作后的公共维护逻辑：
 * 主题最后回复刷新、版块计数刷新等。
 */
@Service
@RequiredArgsConstructor
public class ForumContentMaintenanceService {
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumBoardMapper wfForumBoardMapper;

    public void refreshThreadLastReply(Long threadId) {
        WfForumThread thread = wfForumThreadMapper.selectById(threadId);
        if (thread == null
                || (!ForumThreadStatus.NORMAL.equals(thread.getStatus())
                && !ForumThreadStatus.LOCKED.equals(thread.getStatus()))) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }

        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByDesc(WfForumReply::getFloorNo)
                .last("LIMIT 1");
        WfForumReply lastReply = wfForumReplyMapper.selectOne(queryWrapper);

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId);
        if (lastReply == null) {
            updateWrapper.set(WfForumThread::getLastReplyId, null)
                    .set(WfForumThread::getLastReplyUserId, thread.getAuthorId())
                    .set(WfForumThread::getLastReplyTime, thread.getCreateTime());
        } else {
            updateWrapper.set(WfForumThread::getLastReplyId, lastReply.getId())
                    .set(WfForumThread::getLastReplyUserId, lastReply.getAuthorId())
                    .set(WfForumThread::getLastReplyTime, lastReply.getCreateTime());
        }
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper));
    }

    public void refreshBoardStatsOrThrow(Long boardId) {
        refreshBoardStatsInternal(boardId, false);
    }

    public void refreshBoardStatsIfExists(Long boardId) {
        refreshBoardStatsInternal(boardId, true);
    }

    private void refreshBoardStatsInternal(Long boardId, boolean ignoreMissingBoard) {
        if (ignoreMissingBoard) {
            WfForumBoard board = wfForumBoardMapper.selectById(boardId);
            if (board == null) {
                return;
            }
        }

        Map<String, Object> boardStats = wfForumThreadMapper.selectBoardStats(boardId);
        int threadCount = parseStatsCount(boardStats, "threadCount");
        int replyCount = parseStatsCount(boardStats, "replyCount");
        WfForumThread lastThread = wfForumThreadMapper.selectLatestVisibleByBoardId(boardId);

        LambdaUpdateWrapper<WfForumBoard> boardUpdateWrapper = new LambdaUpdateWrapper<>();
        boardUpdateWrapper.eq(WfForumBoard::getId, boardId)
                .set(WfForumBoard::getThreadCount, threadCount)
                .set(WfForumBoard::getReplyCount, replyCount);
        if (lastThread == null) {
            boardUpdateWrapper.set(WfForumBoard::getLastThreadId, null)
                    .set(WfForumBoard::getLastReplyTime, null);
        } else {
            boardUpdateWrapper.set(WfForumBoard::getLastThreadId, lastThread.getId())
                    .set(WfForumBoard::getLastReplyTime, lastThread.getLastReplyTime());
        }
        int affectedRows = wfForumBoardMapper.update(null, boardUpdateWrapper);
        if (ignoreMissingBoard && affectedRows == 0) {
            return;
        }
        assertSingleRow(affectedRows);
    }

    private int parseStatsCount(Map<String, Object> boardStats, String key) {
        if (boardStats == null) {
            return 0;
        }
        Object value = boardStats.get(key);
        if (!(value instanceof Number)) {
            return 0;
        }
        int count = ((Number) value).intValue();
        return Math.max(count, 0);
    }

    private void assertSingleRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }
}
