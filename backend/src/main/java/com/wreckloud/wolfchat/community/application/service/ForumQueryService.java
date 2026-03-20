package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.storage.service.MediaStorageService;
import com.wreckloud.wolfchat.community.application.assembler.ForumViewAssembler;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadDetailVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import com.wreckloud.wolfchat.community.domain.enums.ForumBoardStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadType;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumBoardMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadLikeMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import com.wreckloud.wolfchat.follow.infra.mapper.WfFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 论坛查询服务：社区主题/回复读取与视图组装。
 */
@Service
@RequiredArgsConstructor
public class ForumQueryService {
    private static final long MIN_PAGE = 1L;
    private static final long MIN_PAGE_SIZE = 1L;
    private static final long MAX_PAGE_SIZE = 50L;
    private static final int MAX_SEARCH_KEYWORD_LENGTH = 40;

    private static final String FEED_TAB_RECOMMEND = "recommend";
    private static final String FEED_TAB_HOT = "hot";
    private static final String FEED_TAB_FRIENDS = "friends";
    private static final String FEED_TAB_LATEST = "latest";
    private static final String REPLY_SORT_FLOOR = "floor";
    private static final String REPLY_SORT_HOT = "hot";
    private static final String REPLY_SORT_AUTHOR = "author";
    private static final String VIDEO_POSTER_PROCESS = "video/snapshot,t_1000,f_jpg,w_480,m_fast";

    private static final String FEED_LATEST_ORDER_SQL = "ORDER BY create_time DESC, id DESC";
    private static final String FEED_HOT_ORDER_SQL = "ORDER BY "
            + "(reply_count * 5 + like_count * 3 + view_count * 0.1 "
            + "+ CASE WHEN TIMESTAMPDIFF(HOUR, COALESCE(last_reply_time, create_time), NOW()) < 24 THEN 20 ELSE 0 END "
            + "- TIMESTAMPDIFF(HOUR, COALESCE(last_reply_time, create_time), NOW()) * 0.2) DESC, "
            + "COALESCE(last_reply_time, create_time) DESC, create_time DESC, id DESC";
    private static final List<String> FEED_EXCLUDED_TITLE_PREFIXES = List.of("[公告]", "【公告】", "[反馈]", "【反馈】");
    private static final int RECOMMEND_INTEREST_RATIO = 60;
    private static final int RECOMMEND_HOT_RATIO = 25;

    private final WfForumBoardMapper wfForumBoardMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfForumThreadLikeMapper wfForumThreadLikeMapper;
    private final WfForumReplyLikeMapper wfForumReplyLikeMapper;
    private final WfFollowMapper wfFollowMapper;
    private final UserService userService;
    private final ForumViewAssembler forumViewAssembler;
    private final MediaStorageService mediaStorageService;

    public ForumThreadPageVO listFeedThreads(Long userId, long page, long size, String tab) {
        validatePageParams(page, size);
        String normalizedTab = normalizeFeedTab(tab);
        if (FEED_TAB_HOT.equals(normalizedTab)) {
            return listHotFeed(userId, page, size);
        }
        if (FEED_TAB_FRIENDS.equals(normalizedTab)) {
            return listFriendsFeed(userId, page, size);
        }
        if (FEED_TAB_LATEST.equals(normalizedTab)) {
            return listLatestFeed(userId, page, size);
        }
        return listRecommendFeed(userId, page, size);
    }

    public ForumThreadPageVO listSearchThreads(Long userId, long page, long size, String keyword) {
        validatePageParams(page, size);
        String normalizedKeyword = normalizeSearchKeyword(keyword);
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery();
        queryWrapper.and(wrapper -> wrapper.like(WfForumThread::getTitle, normalizedKeyword)
                .or()
                .like(WfForumThread::getContent, normalizedKeyword))
                .last(FEED_LATEST_ORDER_SQL);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size), queryWrapper);
        return toThreadPageVO(userId, result.getRecords(), result.getTotal(), page, size);
    }

    public ForumThreadDetailVO getThreadDetail(Long userId, Long threadId) {
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        ForumThreadVO threadVO = buildThreadVO(userId, thread.getId());

        ForumThreadDetailVO detailVO = new ForumThreadDetailVO();
        detailVO.setThread(threadVO);
        detailVO.setContent(thread.getContent());
        return detailVO;
    }

    public ForumReplyPageVO listThreadReplies(Long userId, Long threadId, long page, long size, String sort) {
        validatePageParams(page, size);
        WfForumThread thread = getVisibleThreadOrThrow(threadId);
        String normalizedSort = normalizeReplySort(sort);

        List<WfForumReply> allReplies = loadVisibleReplies(threadId);
        if (allReplies.isEmpty()) {
            return forumViewAssembler.toReplyPageVO(Collections.emptyList(), 0L, page, size);
        }

        Map<Long, Integer> childReplyCountMap = buildChildReplyCountMap(allReplies);
        List<WfForumReply> sortedReplies = filterAndSortReplies(allReplies, thread.getAuthorId(), normalizedSort, childReplyCountMap);
        long total = sortedReplies.size();
        List<WfForumReply> records = sliceReplies(sortedReplies, page, size);
        if (records.isEmpty()) {
            return forumViewAssembler.toReplyPageVO(Collections.emptyList(), total, page, size);
        }

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

        return forumViewAssembler.toReplyPageVO(list, total, page, size);
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
        ForumThreadVO threadVO = forumViewAssembler.toThreadVO(
                thread,
                userMap.get(thread.getAuthorId()),
                userMap.get(thread.getLastReplyUserId()),
                likedByCurrentUser,
                resolveImageUrls(thread.getImageKeys()),
                resolveMediaUrl(thread.getVideoKey()),
                resolveVideoPosterUrl(thread)
        );
        return threadVO;
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

    public WfForumBoard resolveDefaultBoardForPostingOrThrow() {
        LambdaQueryWrapper<WfForumBoard> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumBoard::getStatus, ForumBoardStatus.NORMAL)
                .orderByAsc(WfForumBoard::getSortNo)
                .orderByAsc(WfForumBoard::getId)
                .last("LIMIT 1");
        WfForumBoard board = wfForumBoardMapper.selectOne(queryWrapper);
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

    private ForumThreadPageVO listLatestFeed(Long userId, long page, long size) {
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery()
                .last(FEED_LATEST_ORDER_SQL);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size), queryWrapper);
        return toThreadPageVO(userId, result.getRecords(), result.getTotal(), page, size);
    }

    private ForumThreadPageVO listHotFeed(Long userId, long page, long size) {
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery()
                .last(FEED_HOT_ORDER_SQL);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size), queryWrapper);
        return toThreadPageVO(userId, result.getRecords(), result.getTotal(), page, size);
    }

    private ForumThreadPageVO listFriendsFeed(Long userId, long page, long size) {
        List<Long> friendIds = loadMutualFollowAuthorIds(userId);
        if (friendIds.isEmpty()) {
            return forumViewAssembler.toThreadPageVO(Collections.emptyList(), 0L, page, size);
        }

        LambdaQueryWrapper<WfForumThread> countQueryWrapper = buildFeedVisibleThreadQuery();
        countQueryWrapper.in(WfForumThread::getAuthorId, friendIds);
        Long total = wfForumThreadMapper.selectCount(countQueryWrapper);
        if (total == null || total <= 0L) {
            return forumViewAssembler.toThreadPageVO(Collections.emptyList(), 0L, page, size);
        }

        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery();
        queryWrapper.in(WfForumThread::getAuthorId, friendIds)
                .last(FEED_LATEST_ORDER_SQL);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size, false), queryWrapper);
        return toThreadPageVO(userId, result.getRecords(), total, page, size);
    }

    private ForumThreadPageVO listRecommendFeed(Long userId, long page, long size) {
        int pageSize = (int) size;
        int interestQuota = Math.max(0, Math.round(pageSize * (RECOMMEND_INTEREST_RATIO / 100f)));
        int hotQuota = Math.max(0, Math.round(pageSize * (RECOMMEND_HOT_RATIO / 100f)));
        int exploreQuota = Math.max(0, pageSize - interestQuota - hotQuota);

        List<Long> followingAuthorIds = loadFollowingAuthorIds(userId);
        Set<Long> followingAuthorIdSet = new HashSet<>(followingAuthorIds);

        int interestFetchSize = Math.max(interestQuota * 3, interestQuota + 6);
        int hotFetchSize = Math.max(hotQuota * 3, hotQuota + 6);
        int exploreFetchSize = Math.max(exploreQuota * 3, exploreQuota + 6);

        List<WfForumThread> interestPool = fetchThreadsByAuthors(
                followingAuthorIds,
                page,
                interestFetchSize,
                FEED_LATEST_ORDER_SQL
        );
        List<WfForumThread> hotPool = fetchVisibleThreads(page, hotFetchSize, FEED_HOT_ORDER_SQL);
        List<WfForumThread> explorePool = fetchThreadsExcludeAuthors(
                followingAuthorIdSet,
                page,
                exploreFetchSize,
                FEED_LATEST_ORDER_SQL
        );

        List<WfForumThread> mixedThreads = mixRecommendThreads(
                interestPool,
                hotPool,
                explorePool,
                interestQuota,
                hotQuota,
                exploreQuota,
                pageSize
        );

        if (mixedThreads.size() < pageSize) {
            List<WfForumThread> fallbackPool = fetchVisibleThreads(page, pageSize * 2, FEED_LATEST_ORDER_SQL);
            fillRemainingThreads(mixedThreads, fallbackPool, pageSize);
        }

        Long total = wfForumThreadMapper.selectCount(buildFeedVisibleThreadQuery());
        long safeTotal = total == null ? 0L : total;
        return toThreadPageVO(userId, mixedThreads, safeTotal, page, size);
    }

    private List<WfForumThread> mixRecommendThreads(List<WfForumThread> interestPool,
                                                    List<WfForumThread> hotPool,
                                                    List<WfForumThread> explorePool,
                                                    int interestQuota,
                                                    int hotQuota,
                                                    int exploreQuota,
                                                    int pageSize) {
        LinkedHashMap<Long, WfForumThread> selectedMap = new LinkedHashMap<>();
        int interestIndex = 0;
        int hotIndex = 0;
        int exploreIndex = 0;
        int interestPicked = 0;
        int hotPicked = 0;
        int explorePicked = 0;

        while (selectedMap.size() < pageSize) {
            boolean progressed = false;

            if (interestPicked < interestQuota) {
                WfForumThread next = pollNextUniqueThread(interestPool, selectedMap, interestIndex);
                if (next != null) {
                    selectedMap.put(next.getId(), next);
                    interestPicked++;
                    interestIndex = findNextIndex(interestPool, selectedMap, interestIndex);
                    progressed = true;
                } else {
                    interestPicked = interestQuota;
                }
            }

            if (selectedMap.size() >= pageSize) {
                break;
            }

            if (hotPicked < hotQuota) {
                WfForumThread next = pollNextUniqueThread(hotPool, selectedMap, hotIndex);
                if (next != null) {
                    selectedMap.put(next.getId(), next);
                    hotPicked++;
                    hotIndex = findNextIndex(hotPool, selectedMap, hotIndex);
                    progressed = true;
                } else {
                    hotPicked = hotQuota;
                }
            }

            if (selectedMap.size() >= pageSize) {
                break;
            }

            if (explorePicked < exploreQuota) {
                WfForumThread next = pollNextUniqueThread(explorePool, selectedMap, exploreIndex);
                if (next != null) {
                    selectedMap.put(next.getId(), next);
                    explorePicked++;
                    exploreIndex = findNextIndex(explorePool, selectedMap, exploreIndex);
                    progressed = true;
                } else {
                    explorePicked = exploreQuota;
                }
            }

            if (!progressed) {
                break;
            }
        }

        if (selectedMap.size() < pageSize) {
            fillRemainingThreads(
                    new ArrayList<>(selectedMap.values()),
                    mergeThreadPools(interestPool, hotPool, explorePool),
                    pageSize,
                    selectedMap
            );
            return new ArrayList<>(selectedMap.values());
        }
        return new ArrayList<>(selectedMap.values());
    }

    private List<WfForumThread> mergeThreadPools(List<WfForumThread> interestPool,
                                                 List<WfForumThread> hotPool,
                                                 List<WfForumThread> explorePool) {
        List<WfForumThread> merged = new ArrayList<>();
        if (interestPool != null) {
            merged.addAll(interestPool);
        }
        if (hotPool != null) {
            merged.addAll(hotPool);
        }
        if (explorePool != null) {
            merged.addAll(explorePool);
        }
        return merged;
    }

    private WfForumThread pollNextUniqueThread(List<WfForumThread> pool,
                                               Map<Long, WfForumThread> selectedMap,
                                               int startIndex) {
        if (pool == null || pool.isEmpty()) {
            return null;
        }
        for (int index = Math.max(0, startIndex); index < pool.size(); index++) {
            WfForumThread candidate = pool.get(index);
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (selectedMap.containsKey(candidate.getId())) {
                continue;
            }
            return candidate;
        }
        return null;
    }

    private int findNextIndex(List<WfForumThread> pool,
                              Map<Long, WfForumThread> selectedMap,
                              int startIndex) {
        if (pool == null || pool.isEmpty()) {
            return 0;
        }
        for (int index = Math.max(0, startIndex); index < pool.size(); index++) {
            WfForumThread candidate = pool.get(index);
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (!selectedMap.containsKey(candidate.getId())) {
                return index;
            }
        }
        return pool.size();
    }

    private void fillRemainingThreads(List<WfForumThread> current,
                                      List<WfForumThread> candidates,
                                      int limit) {
        LinkedHashMap<Long, WfForumThread> selectedMap = new LinkedHashMap<>();
        for (WfForumThread item : current) {
            if (item == null || item.getId() == null) {
                continue;
            }
            selectedMap.put(item.getId(), item);
        }
        fillRemainingThreads(current, candidates, limit, selectedMap);
    }

    private void fillRemainingThreads(List<WfForumThread> current,
                                      List<WfForumThread> candidates,
                                      int limit,
                                      LinkedHashMap<Long, WfForumThread> selectedMap) {
        if (selectedMap.size() >= limit || candidates == null || candidates.isEmpty()) {
            return;
        }
        for (WfForumThread candidate : candidates) {
            if (candidate == null || candidate.getId() == null) {
                continue;
            }
            if (selectedMap.containsKey(candidate.getId())) {
                continue;
            }
            selectedMap.put(candidate.getId(), candidate);
            if (selectedMap.size() >= limit) {
                break;
            }
        }
        current.clear();
        current.addAll(selectedMap.values());
    }

    private List<WfForumThread> fetchVisibleThreads(long page, long size, String orderSql) {
        if (size <= 0L) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery().last(orderSql);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size, false), queryWrapper);
        return result.getRecords();
    }

    private List<WfForumThread> fetchThreadsByAuthors(List<Long> authorIds, long page, long size, String orderSql) {
        if (authorIds == null || authorIds.isEmpty() || size <= 0L) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery();
        queryWrapper.in(WfForumThread::getAuthorId, authorIds).last(orderSql);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size, false), queryWrapper);
        return result.getRecords();
    }

    private List<WfForumThread> fetchThreadsExcludeAuthors(Set<Long> excludedAuthorIds, long page, long size, String orderSql) {
        if (size <= 0L) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildFeedVisibleThreadQuery();
        if (excludedAuthorIds != null && !excludedAuthorIds.isEmpty()) {
            queryWrapper.notIn(WfForumThread::getAuthorId, excludedAuthorIds);
        }
        queryWrapper.last(orderSql);
        Page<WfForumThread> result = wfForumThreadMapper.selectPage(new Page<>(page, size, false), queryWrapper);
        return result.getRecords();
    }

    private LambdaQueryWrapper<WfForumThread> buildVisibleThreadQuery() {
        LambdaQueryWrapper<WfForumThread> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.ne(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        return queryWrapper;
    }

    private LambdaQueryWrapper<WfForumThread> buildFeedVisibleThreadQuery() {
        LambdaQueryWrapper<WfForumThread> queryWrapper = buildVisibleThreadQuery();
        queryWrapper.ne(WfForumThread::getThreadType, ForumThreadType.ANNOUNCEMENT);
        for (String titlePrefix : FEED_EXCLUDED_TITLE_PREFIXES) {
            queryWrapper.notLikeRight(WfForumThread::getTitle, titlePrefix);
        }
        return queryWrapper;
    }

    private List<Long> loadFollowingAuthorIds(Long userId) {
        if (userId == null || userId <= 0L) {
            return Collections.emptyList();
        }
        List<Long> ids = wfFollowMapper.selectFollowingIdsByUserId(userId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0L)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<Long> loadMutualFollowAuthorIds(Long userId) {
        if (userId == null || userId <= 0L) {
            return Collections.emptyList();
        }
        List<Long> ids = wfFollowMapper.selectMutualFollowIdsByUserId(userId);
        if (ids == null || ids.isEmpty()) {
            return Collections.emptyList();
        }
        return ids.stream()
                .filter(id -> id != null && id > 0L)
                .distinct()
                .collect(Collectors.toList());
    }

    private String normalizeFeedTab(String tab) {
        if (!StringUtils.hasText(tab)) {
            return FEED_TAB_RECOMMEND;
        }
        String normalizedTab = tab.trim().toLowerCase();
        if (FEED_TAB_RECOMMEND.equals(normalizedTab)
                || FEED_TAB_HOT.equals(normalizedTab)
                || FEED_TAB_FRIENDS.equals(normalizedTab)
                || FEED_TAB_LATEST.equals(normalizedTab)) {
            return normalizedTab;
        }
        throw new BaseException(ErrorCode.PARAM_ERROR, "tab 参数不合法，仅支持 recommend/hot/friends/latest");
    }

    private String normalizeReplySort(String sort) {
        if (!StringUtils.hasText(sort)) {
            return REPLY_SORT_FLOOR;
        }
        String normalizedSort = sort.trim().toLowerCase();
        if (REPLY_SORT_FLOOR.equals(normalizedSort)
                || REPLY_SORT_HOT.equals(normalizedSort)
                || REPLY_SORT_AUTHOR.equals(normalizedSort)) {
            return normalizedSort;
        }
        throw new BaseException(ErrorCode.PARAM_ERROR, "sort 参数不合法，仅支持 floor/hot/author");
    }

    private List<WfForumReply> loadVisibleReplies(Long threadId) {
        LambdaQueryWrapper<WfForumReply> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfForumReply::getThreadId, threadId)
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .orderByAsc(WfForumReply::getFloorNo);
        return wfForumReplyMapper.selectList(queryWrapper);
    }

    private Map<Long, Integer> buildChildReplyCountMap(List<WfForumReply> replies) {
        if (replies == null || replies.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> countMap = new LinkedHashMap<>();
        for (WfForumReply reply : replies) {
            Long parentId = reply.getQuoteReplyId();
            if (parentId == null || parentId <= 0L) {
                continue;
            }
            countMap.put(parentId, countMap.getOrDefault(parentId, 0) + 1);
        }
        return countMap;
    }

    private List<WfForumReply> filterAndSortReplies(List<WfForumReply> allReplies,
                                                    Long threadAuthorId,
                                                    String sort,
                                                    Map<Long, Integer> childReplyCountMap) {
        List<WfForumReply> target = new ArrayList<>(allReplies.size());
        if (REPLY_SORT_AUTHOR.equals(sort)) {
            for (WfForumReply reply : allReplies) {
                if (threadAuthorId != null && threadAuthorId.equals(reply.getAuthorId())) {
                    target.add(reply);
                }
            }
            return target;
        }

        target.addAll(allReplies);
        if (REPLY_SORT_HOT.equals(sort)) {
            target.sort(Comparator
                    .comparingInt((WfForumReply reply) -> resolveReplyHeat(reply, childReplyCountMap)).reversed()
                    .thenComparing(WfForumReply::getFloorNo, Comparator.nullsLast(Integer::compareTo))
                    .thenComparing(WfForumReply::getId, Comparator.nullsLast(Long::compareTo)));
        }
        return target;
    }

    private int resolveReplyHeat(WfForumReply reply, Map<Long, Integer> childReplyCountMap) {
        int likeCount = normalizeNonNegativeInt(reply.getLikeCount());
        int childReplyCount = childReplyCountMap.getOrDefault(reply.getId(), 0);
        return likeCount + childReplyCount;
    }

    private int normalizeNonNegativeInt(Integer value) {
        return value == null || value < 0 ? 0 : value;
    }

    private List<WfForumReply> sliceReplies(List<WfForumReply> sortedReplies, long page, long size) {
        if (sortedReplies == null || sortedReplies.isEmpty()) {
            return Collections.emptyList();
        }
        long offset = (page - 1L) * size;
        if (offset >= sortedReplies.size()) {
            return Collections.emptyList();
        }
        int fromIndex = (int) Math.max(offset, 0L);
        int toIndex = (int) Math.min(offset + size, sortedReplies.size());
        return new ArrayList<>(sortedReplies.subList(fromIndex, toIndex));
    }

    private ForumThreadPageVO toThreadPageVO(Long userId,
                                             List<WfForumThread> records,
                                             long total,
                                             long page,
                                             long size) {
        if (records == null || records.isEmpty()) {
            return forumViewAssembler.toThreadPageVO(Collections.emptyList(), total, page, size);
        }
        Map<Long, WfUser> userMap = loadThreadUserMap(records);
        Set<Long> likedThreadIds = loadLikedThreadIds(userId, records);

        List<ForumThreadVO> list = new ArrayList<>(records.size());
        for (WfForumThread thread : records) {
            WfUser author = userMap.get(thread.getAuthorId());
            WfUser lastReplyUser = userMap.get(thread.getLastReplyUserId());
            ForumThreadVO threadVO = forumViewAssembler.toThreadVO(
                    thread,
                    author,
                    lastReplyUser,
                    likedThreadIds.contains(thread.getId()),
                    resolveImageUrls(thread.getImageKeys()),
                    resolveMediaUrl(thread.getVideoKey()),
                    resolveVideoPosterUrl(thread)
            );
            list.add(threadVO);
        }
        return forumViewAssembler.toThreadPageVO(list, total, page, size);
    }

    private void validatePageParams(long page, long size) {
        if (page < MIN_PAGE || size < MIN_PAGE_SIZE || size > MAX_PAGE_SIZE) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "分页参数不合法，page>=1 且 size 在1-50之间");
        }
    }

    private String normalizeSearchKeyword(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "关键词不能为空");
        }
        String normalized = keyword.trim();
        if (normalized.length() > MAX_SEARCH_KEYWORD_LENGTH) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "关键词长度不能超过40个字符");
        }
        return normalized;
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
        return mediaStorageService.buildSignedReadUrl(mediaKey.trim());
    }

    private String resolveVideoPosterUrl(WfForumThread thread) {
        if (thread == null) {
            return null;
        }
        if (StringUtils.hasText(thread.getVideoPosterKey())) {
            return resolveMediaUrl(thread.getVideoPosterKey());
        }
        if (!StringUtils.hasText(thread.getVideoKey())) {
            return null;
        }
        return null;
    }

}
