package com.wreckloud.wolfchat.community.application.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Description 论坛点赞写路径支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
@RequiredArgsConstructor
public class ForumLikeCommandSupport {
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final WfForumReplyMapper wfForumReplyMapper;

    public boolean likeThread(Long userId, Long threadId) {
        if (existsThreadLike(userId, threadId)) {
            return false;
        }
        WfForumThreadLike like = new WfForumThreadLike();
        like.setThreadId(threadId);
        like.setUserId(userId);
        assertSingleRow(wfForumThreadLikeMapper.insert(like));

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .setSql("like_count = like_count + 1");
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
        return true;
    }

    public void unlikeThread(Long userId, Long threadId) {
        LambdaQueryWrapper<WfForumThreadLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumThreadLike::getUserId, userId)
                .eq(WfForumThreadLike::getThreadId, threadId);
        int deletedRows = wfForumThreadLikeMapper.delete(deleteWrapper);
        if (deletedRows == 0) {
            return;
        }

        LambdaUpdateWrapper<WfForumThread> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumThread::getId, threadId)
                .in(WfForumThread::getStatus, List.of(ForumThreadStatus.NORMAL, ForumThreadStatus.LOCKED))
                .setSql("like_count = IF(like_count > 0, like_count - 1, 0)");
        assertSingleRow(wfForumThreadMapper.update(null, updateWrapper), ErrorCode.FORUM_THREAD_NOT_FOUND);
    }

    public boolean likeReply(Long userId, Long replyId) {
        if (existsReplyLike(userId, replyId)) {
            return false;
        }
        WfForumReplyLike like = new WfForumReplyLike();
        like.setReplyId(replyId);
        like.setUserId(userId);
        assertSingleRow(wfForumReplyLikeMapper.insert(like));

        LambdaUpdateWrapper<WfForumReply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .setSql("like_count = like_count + 1");
        assertSingleRow(wfForumReplyMapper.update(null, updateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);
        return true;
    }

    public void unlikeReply(Long userId, Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> deleteWrapper = new LambdaQueryWrapper<>();
        deleteWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId);
        int deletedRows = wfForumReplyLikeMapper.delete(deleteWrapper);
        if (deletedRows == 0) {
            return;
        }

        LambdaUpdateWrapper<WfForumReply> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfForumReply::getId, replyId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .setSql("like_count = IF(like_count > 0, like_count - 1, 0)");
        assertSingleRow(wfForumReplyMapper.update(null, updateWrapper), ErrorCode.FORUM_REPLY_NOT_FOUND);
    }

    private boolean existsThreadLike(Long userId, Long threadId) {
        LambdaQueryWrapper<WfForumThreadLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThreadLike::getUserId, userId)
                .eq(WfForumThreadLike::getThreadId, threadId)
                .last("LIMIT 1");
        Long count = wfForumThreadLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private boolean existsReplyLike(Long userId, Long replyId) {
        LambdaQueryWrapper<WfForumReplyLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId)
                .last("LIMIT 1");
        Long count = wfForumReplyLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private void assertSingleRow(int affectedRows) {
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private void assertSingleRow(int affectedRows, ErrorCode zeroRowErrorCode) {
        if (affectedRows == 0) {
            throw new BaseException(zeroRowErrorCode);
        }
        if (affectedRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }
}

