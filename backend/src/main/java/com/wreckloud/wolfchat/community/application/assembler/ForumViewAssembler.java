package com.wreckloud.wolfchat.community.application.assembler;

import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.community.api.vo.ForumBoardVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.api.vo.UserBriefVO;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 论坛视图装配：实体与 API 视图对象转换。
 */
@Component
public class ForumViewAssembler {

    public ForumBoardVO toBoardVO(WfForumBoard board) {
        ForumBoardVO vo = new ForumBoardVO();
        vo.setBoardId(board.getId());
        vo.setName(board.getName());
        vo.setSlug(board.getSlug());
        vo.setDescription(board.getDescription());
        vo.setSortNo(board.getSortNo());
        vo.setStatus(board.getStatus());
        vo.setThreadCount(normalizeCount(board.getThreadCount()));
        vo.setReplyCount(normalizeCount(board.getReplyCount()));
        vo.setLastReplyTime(board.getLastReplyTime());
        return vo;
    }

    public ForumThreadPageVO toThreadPageVO(List<ForumThreadVO> list, long total, long page, long size) {
        ForumThreadPageVO pageVO = new ForumThreadPageVO();
        pageVO.setList(list);
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    public ForumReplyPageVO toReplyPageVO(List<ForumReplyVO> list, long total, long page, long size) {
        ForumReplyPageVO pageVO = new ForumReplyPageVO();
        pageVO.setList(list);
        pageVO.setTotal(total);
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    public ForumThreadVO toThreadVO(WfForumThread thread, WfUser author, WfUser lastReplyUser, boolean likedByCurrentUser) {
        ForumThreadVO vo = new ForumThreadVO();
        vo.setThreadId(thread.getId());
        vo.setBoardId(thread.getBoardId());
        vo.setTitle(thread.getTitle());
        vo.setThreadType(thread.getThreadType());
        vo.setStatus(thread.getStatus());
        vo.setIsEssence(Boolean.TRUE.equals(thread.getIsEssence()));
        vo.setViewCount(normalizeCount(thread.getViewCount()));
        vo.setReplyCount(normalizeCount(thread.getReplyCount()));
        vo.setLikeCount(normalizeCount(thread.getLikeCount()));
        vo.setLikedByCurrentUser(likedByCurrentUser);
        vo.setLastReplyTime(thread.getLastReplyTime());
        vo.setCreateTime(thread.getCreateTime());
        vo.setAuthor(toUserBriefVO(author));
        vo.setLastReplyUser(toUserBriefVO(lastReplyUser));
        return vo;
    }

    public ForumThreadVO toThreadVO(WfForumThread thread, WfUser author, WfUser lastReplyUser) {
        return toThreadVO(thread, author, lastReplyUser, false);
    }

    public ForumReplyVO toReplyVO(WfForumReply reply, WfUser author, WfForumReply quoteReply, WfUser quoteAuthor, boolean likedByCurrentUser) {
        ForumReplyVO vo = new ForumReplyVO();
        vo.setReplyId(reply.getId());
        vo.setThreadId(reply.getThreadId());
        vo.setFloorNo(reply.getFloorNo());
        vo.setContent(reply.getContent());
        vo.setCreateTime(reply.getCreateTime());
        vo.setLikeCount(normalizeCount(reply.getLikeCount()));
        vo.setLikedByCurrentUser(likedByCurrentUser);
        vo.setAuthor(toUserBriefVO(author));
        if (quoteReply != null) {
            vo.setQuoteReplyId(quoteReply.getId());
            vo.setQuoteFloorNo(quoteReply.getFloorNo());
            vo.setQuoteAuthor(toUserBriefVO(quoteAuthor));
            vo.setQuoteContent(quoteReply.getContent());
        }
        return vo;
    }

    public ForumReplyVO toReplyVO(WfForumReply reply, WfUser author, WfForumReply quoteReply, WfUser quoteAuthor) {
        return toReplyVO(reply, author, quoteReply, quoteAuthor, false);
    }

    private UserBriefVO toUserBriefVO(WfUser user) {
        if (user == null) {
            return null;
        }
        UserBriefVO vo = new UserBriefVO();
        vo.setUserId(user.getId());
        vo.setWolfNo(user.getWolfNo());
        vo.setNickname(user.getNickname());
        vo.setEquippedTitleName(user.getEquippedTitleName());
        vo.setEquippedTitleColor(user.getEquippedTitleColor());
        vo.setAvatar(user.getAvatar());
        return vo;
    }

    private int normalizeCount(Integer count) {
        return count == null || count < 0 ? 0 : count;
    }
}
