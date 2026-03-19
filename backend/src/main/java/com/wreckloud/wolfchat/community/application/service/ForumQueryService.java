package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.service.OssStorageService;
import com.wreckloud.wolfchat.community.application.assembler.ForumViewAssembler;
import com.wreckloud.wolfchat.community.api.vo.ForumBoardVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadDetailVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.api.vo.UserBriefVO;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 论坛查询服务：版块/主题/回复读取与视图组装。
 */
@Service
@RequiredArgsConstructor
public class ForumQueryService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_PAGE_SIZE = 1L;
    private static final long MAX_PAGE_SIZE = 50L;

    private static final String TAB_ALL = "all";
    private static final String TAB_STICKY = "sticky";
    private static final String TAB_ESSENCE = "essence";

    private static final String THREAD_LIST_ORDER_SQL = "ORDER BY "
            + "CASE thread_type WHEN 'ANNOUNCEMENT' THEN 2 WHEN 'STICKY' THEN 1 ELSE 0 END DESC, "
            + "last_reply_time DESC, create_time DESC, id DESC";

    private final WfForumBoardMapper wfForumBoardMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final UserService userService;
    private final ForumViewAssembler forumViewAssembler;
    private final OssStorageService ossStorageService;

    public List<ForumBoardVO> listBoards() {
        LambdaQueryWrapper<WfForumBoard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByAsc(WfForumBoard::getSortNo)
                .orderByAsc(WfForumBoard::getId);
        List<WfForumBoard> boards = wfForumBoardMapper.selectList(queryWrapper);
        if (boards.isEmpty()) {
            return Collections.emptyList();
        }

        List<ForumBoardVO> list = new ArrayList<>(boards.size());
        for (WfForumBoard board : boards) {
            list.add(forumViewAssembler.toBoardVO(board));
        }
        return list;
    }

    public ForumThreadPageVO listBoardThreads(Long userId, Long boardId, long page, long size, String tab) {
        validatePageParams(page, size);
        getBoardOrThrow(boardId);
        String normalizedTab = normalizeTab(tab);

        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThread::getBoardId, boardId)
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        applyThreadTabFilter(normalizedTab, queryWrapper);
        queryWrapper.last(THREAD_LIST_ORDER_SQL);

        Page<WfForumThread> pageReq = new Page<>(page, size);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(pageReq, queryWrapper);

        List<WfForumThread> records = result.getRecords();
        Map<Long, WfUser> userMap = loadThreadUserMap(records);
        Set<Long> likedThreadIds = loadLikedThreadIds(userId, records);
        List<ForumThreadVO> list = new ArrayList<>(records.size());
        for (WfForumThread thread : records) {
            WfUser author = userMap.get(thread.getAuthorId());
            WfUser lastReplyUser = userMap.get(thread.getLastReplyUserId());
            list.add(forumViewAssembler.toThreadVO(
                    thread,
                    author,
                    lastReplyUser,
                    likedThreadIds.contains(thread.getId()),
                    resolveImageUrls(thread.getImageKeys()),
                    resolveMediaUrl(thread.getVideoKey())
            ));
        }

        return forumViewAssembler.toThreadPageVO(list, result.getTotal(), page, size);
    }

    public ForumThreadDetailVO getThreadDetail(Long userId, Long threadId) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        ForumThreadVO threadVO = buildThreadVO(userId, thread.getId());

        ForumThreadDetailVO detailVO = new ForumThreadDetailVO();
        detailVO.setThread(threadVO);
        detailVO.setContent(thread.getContent());
        return detailVO;
    }

    public ForumReplyPageVO listThreadReplies(Long userId, Long threadId, long page, long size) {
        validatePageParams(page, size);
        getVisibleThreadOrThrow(threadId);

        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByAsc(WfForumReply::getFloorNo);

        Page<WfForumReply> pageReq = new Page<>(page, size);
        Page<WfForumReply> result = wfForumReplyMapper.selectPage(pageReq, queryWrapper);

        List<WfForumReply> records = result.getRecords();
        Map<Long, WfForumReply> quoteReplyMap = loadQuoteReplyMap(threadId, records);
        Map<Long, WfUser> userMap = loadReplyUserMap(records, quoteReplyMap);
        Set<Long> likedReplyIds = loadLikedReplyIds(userId, records);

        List<ForumReplyVO> list = new ArrayList<>(records.size());
        for (WfForumReply reply : records) {
            WfUser author = userMap.get(reply.getAuthorId());
            WfForumReply quoteReply = quoteReplyMap.get(reply.getQuoteReplyId());
            WfUser quoteAuthor = quoteReply == null ? null : userMap.get(quoteReply.getAuthorId());
            list.add(forumViewAssembler.toReplyVO(
                    reply,
                    author,
                    quoteReply,
                    quoteAuthor,
                    likedReplyIds.contains(reply.getId()),
                    resolveMediaUrl(reply.getImageKey())
            ));
        }

        return forumViewAssembler.toReplyPageVO(list, result.getTotal(), page, size);
    }

    public ForumThreadVO buildThreadVO(Long userId, Long threadId) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        Set<Long> userIds = new HashSet<>();
        if (thread.getAuthorId() != null) {
            userIds.add(thread.getAuthorId());
        }
        if (thread.getLastReplyUserId() != null) {
            userIds.add(thread.getLastReplyUserId());
        }
        Map<Long, WfUser> userMap = loadUserMap(userIds);
        boolean likedByCurrentUser = isThreadLikedByUser(userId, threadId);
        return forumViewAssembler.toThreadVO(
                thread,
                userMap.get(thread.getAuthorId()),
                userMap.get(thread.getLastReplyUserId()),
                likedByCurrentUser,
                resolveImageUrls(thread.getImageKeys()),
                resolveMediaUrl(thread.getVideoKey())
        );
    }

    public ForumReplyVO buildReplyVO(Long userId, Long replyId) {
        WfForumReply reply = getVisibleReplyOrThrow(replyId);
        WfForumReply quoteReply = loadVisibleQuoteReply(reply.getThreadId(), reply.getQuoteReplyId());

        Set<Long> userIds = new HashSet<>();
        if (reply.getAuthorId() != null) {
            userIds.add(reply.getAuthorId());
        }
        if (quoteReply != null && quoteReply.getAuthorId() != null) {
            userIds.add(quoteReply.getAuthorId());
        }
        Map<Long, WfUser> userMap = loadUserMap(userIds);
        WfUser author = userMap.get(reply.getAuthorId());
        WfUser quoteAuthor = quoteReply == null ? null : userMap.get(quoteReply.getAuthorId());
        boolean likedByCurrentUser = isReplyLikedByUser(userId, replyId);
        return forumViewAssembler.toReplyVO(
                reply,
                author,
                quoteReply,
                quoteAuthor,
                likedByCurrentUser,
                resolveMediaUrl(reply.getImageKey())
        );
    }

    public WfForumBoard getBoardOrThrow(Long boardId) {
        WfForumBoard board = wfForumBoardMapper.selectById(boardId);
        if (board == null) {
            throw new BaseException(ErrorCode.FORUM_BOARD_NOT_FOUND);
        }
        return board;
    }

    public WfForumThread getVisibleThreadOrThrow(Long threadId) {
        WfForumThread thread = wfForumThreadMapper.selectById(threadId);
        if (thread == null || ForumThreadStatus.DELETED.equals(thread.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_THREAD_NOT_FOUND);
        }
        return thread;
    }

    public WfForumReply getVisibleReplyOrThrow(Long replyId) {
        WfForumReply reply = wfForumReplyMapper.selectById(replyId);
        if (reply == null || ForumReplyStatus.DELETED.equals(reply.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_REPLY_NOT_FOUND);
        }
        return reply;
    }

    public WfForumReply getQuoteReplyOrThrow(Long threadId, Long quoteReplyId) {
        WfForumReply quoteReply = wfForumReplyMapper.selectById(quoteReplyId);
        if (quoteReply == null
                || !threadId.equals(quoteReply.getThreadId())
                || ForumReplyStatus.DELETED.equals(quoteReply.getStatus())) {
            throw new BaseException(ErrorCode.FORUM_REPLY_NOT_FOUND);
        }
        return quoteReply;
    }

    public boolean isThreadLikedByUser(Long userId, Long threadId) {
        if (userId == null || threadId == null) {
            return false;
        }
        LambdaQueryWrapper<WfForumThreadLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumThreadLike::getUserId, userId)
                .eq(WfForumThreadLike::getThreadId, threadId)
                .last("LIMIT 1");
        Long count = wfForumThreadLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    public boolean isReplyLikedByUser(Long userId, Long replyId) {
        if (userId == null || replyId == null) {
            return false;
        }
        LambdaQueryWrapper<WfForumReplyLike> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReplyLike::getUserId, userId)
                .eq(WfForumReplyLike::getReplyId, replyId)
                .last("LIMIT 1");
        Long count = wfForumReplyLikeMapper.selectCount(queryWrapper);
        return count != null && count > 0;
    }

    private void validatePageParams(long page, long size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "分页参数不合法，page>=1 且 size 在1-50之间");
        }
    }

    private String normalizeTab(String tab) {
        if (!StringUtils.hasText(tab)) {
            return TAB_ALL;
        }
        String normalizedTab = tab.trim().toLowerCase();
        if (TAB_ALL.equals(normalizedTab) || TAB_STICKY.equals(normalizedTab) || TAB_ESSENCE.equals(normalizedTab)) {
            return normalizedTab;
        }
        throw new BaseException(ErrorCode.PARAM_ERROR, "tab 参数不合法，仅支持 all/sticky/essence");
    }

    private void applyThreadTabFilter(String tab, LambdaQueryWrapper<WfForumThread> queryWrapper) {
        if (TAB_STICKY.equals(tab)) {
            queryWrapper.in(WfForumThread::getThreadType, ForumThreadType.STICKY, ForumThreadType.ANNOUNCEMENT);
            return;
        }
        if (TAB_ESSENCE.equals(tab)) {
            queryWrapper.eq(WfForumThread::getIsEssence, true);
        }
    }

    private WfForumReply loadVisibleQuoteReply(Long threadId, Long quoteReplyId) {
        if (quoteReplyId == null || quoteReplyId <= 0) {
            return null;
        }
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getId, quoteReplyId)
                .eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .last("LIMIT 1");
        return wfForumReplyMapper.selectOne(queryWrapper);
    }

    private Map<Long, WfUser> loadUserMap(Set<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userService.getUserMap(new ArrayList<>(userIds));
    }

    private Map<Long, WfUser> loadThreadUserMap(List<WfForumThread> threads) {
        if (threads.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = new HashSet<>();
        for (WfForumThread thread : threads) {
            if (thread.getAuthorId() != null) {
                userIds.add(thread.getAuthorId());
            }
            if (thread.getLastReplyUserId() != null) {
                userIds.add(thread.getLastReplyUserId());
            }
        }
        return loadUserMap(userIds);
    }

    private Map<Long, WfForumReply> loadQuoteReplyMap(Long threadId, List<WfForumReply> replies) {
        if (replies.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> quoteReplyIds = replies.stream()
                .map(WfForumReply::getQuoteReplyId)
                .filter(id -> id != null && id > 0)
                .distinct()
                .collect(Collectors.toList());
        if (quoteReplyIds.isEmpty()) {
            return Collections.emptyMap();
        }

        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(WfForumReply::getId, quoteReplyIds)
                .eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL);
        List<WfForumReply> quoteReplies = wfForumReplyMapper.selectList(queryWrapper);
        if (quoteReplies.isEmpty()) {
            return Collections.emptyMap();
        }
        return quoteReplies.stream().collect(Collectors.toMap(WfForumReply::getId, item -> item));
    }

    private Map<Long, WfUser> loadReplyUserMap(List<WfForumReply> replies, Map<Long, WfForumReply> quoteReplyMap) {
        if (replies.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> userIds = new HashSet<>();
        for (WfForumReply reply : replies) {
            if (reply.getAuthorId() != null) {
                userIds.add(reply.getAuthorId());
            }
            WfForumReply quoteReply = quoteReplyMap.get(reply.getQuoteReplyId());
            if (quoteReply != null && quoteReply.getAuthorId() != null) {
                userIds.add(quoteReply.getAuthorId());
            }
        }
        return loadUserMap(userIds);
    }

    private Set<Long> loadLikedThreadIds(Long userId, List<WfForumThread> threads) {
        if (userId == null || threads == null || threads.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> threadIds = threads.stream()
                .map(WfForumThread::getId)
                .distinct()
                .collect(Collectors.toList());
        if (threadIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> likedIds = wfForumThreadLikeMapper.selectLikedThreadIds(userId, threadIds);
        if (likedIds == null || likedIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(likedIds);
    }

    private Set<Long> loadLikedReplyIds(Long userId, List<WfForumReply> replies) {
        if (userId == null || replies == null || replies.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> replyIds = replies.stream()
                .map(WfForumReply::getId)
                .distinct()
                .collect(Collectors.toList());
        if (replyIds.isEmpty()) {
            return Collections.emptySet();
        }
        List<Long> likedIds = wfForumReplyLikeMapper.selectLikedReplyIds(userId, replyIds);
        if (likedIds == null || likedIds.isEmpty()) {
            return Collections.emptySet();
        }
        return new HashSet<>(likedIds);
    }

    private List<String> resolveImageUrls(String imageKeys) {
        if (!StringUtils.hasText(imageKeys)) {
            return List.of();
        }
        return Arrays.stream(imageKeys.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::resolveMediaUrl)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }

    private String resolveMediaUrl(String mediaKey) {
        if (!StringUtils.hasText(mediaKey)) {
            return null;
        }
        return ossStorageService.buildSignedReadUrl(mediaKey.trim());
    }

}
