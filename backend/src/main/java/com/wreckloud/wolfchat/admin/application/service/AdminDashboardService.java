package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfLoginRecord;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfLoginRecordMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardClientTypeVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardOverviewVO;
import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardTrendVO;
import com.wreckloud.wolfchat.chat.lobby.domain.entity.WfLobbyMessage;
import com.wreckloud.wolfchat.chat.lobby.infra.mapper.WfLobbyMessageMapper;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 管理端控制台服务。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private static final int DEFAULT_TREND_DAYS = 7;
    private static final DateTimeFormatter DATE_LABEL_FORMATTER = DateTimeFormatter.ofPattern("MM-dd");

    private final WfUserMapper wfUserMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfLoginRecordMapper wfLoginRecordMapper;
    private final WfLobbyMessageMapper wfLobbyMessageMapper;
    private final UserPresenceService userPresenceService;

    public AdminDashboardOverviewVO getOverview() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        LocalDateTime twentyFourHoursAgo = now.minusHours(24);

        long userCount = wfUserMapper.selectCount(new LambdaQueryWrapper<>());
        long activeUser7d = wfUserMapper.selectCount(new LambdaQueryWrapper<WfUser>()
                .eq(WfUser::getStatus, UserStatus.NORMAL)
                .ge(WfUser::getLastLoginAt, sevenDaysAgo));

        LambdaQueryWrapper<WfForumThread> visibleThreadWrapper = new LambdaQueryWrapper<WfForumThread>()
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED);
        long threadCount = wfForumThreadMapper.selectCount(visibleThreadWrapper);
        long todayThreadCount = wfForumThreadMapper.selectCount(new LambdaQueryWrapper<WfForumThread>()
                .ne(WfForumThread::getStatus, ForumThreadStatus.DELETED)
                .ge(WfForumThread::getCreateTime, dayStart));

        long replyCount = wfForumReplyMapper.selectCount(new LambdaQueryWrapper<WfForumReply>()
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL));
        long todayReplyCount = wfForumReplyMapper.selectCount(new LambdaQueryWrapper<WfForumReply>()
                .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                .ge(WfForumReply::getCreateTime, dayStart));

        long loginFail24h = wfLoginRecordMapper.selectCount(new LambdaQueryWrapper<WfLoginRecord>()
                .eq(WfLoginRecord::getLoginResult, LoginResult.FAIL)
                .ge(WfLoginRecord::getLoginTime, twentyFourHoursAgo));

        AdminDashboardOverviewVO overviewVO = new AdminDashboardOverviewVO();
        overviewVO.setUserCount(userCount);
        overviewVO.setActiveUser7d(activeUser7d);
        overviewVO.setThreadCount(threadCount);
        overviewVO.setReplyCount(replyCount);
        overviewVO.setOnlineUserCount(userPresenceService.countOnlineUsers());
        overviewVO.setLoginFail24h(loginFail24h);
        overviewVO.setTodayThreadCount(todayThreadCount);
        overviewVO.setTodayReplyCount(todayReplyCount);
        return overviewVO;
    }

    public AdminDashboardTrendVO getTrend(int days) {
        int trendDays = normalizeTrendDays(days);
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(trendDays - 1L);
        LocalDateTime startAt = startDate.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1L).atStartOfDay();

        Map<LocalDate, Long> registerCountMap = aggregateByDate(
                wfUserMapper.selectList(new LambdaQueryWrapper<WfUser>()
                        .ge(WfUser::getCreateTime, startAt)
                        .lt(WfUser::getCreateTime, endAt)),
                WfUser::getCreateTime
        );
        Map<LocalDate, Long> threadCountMap = aggregateByDate(
                wfForumThreadMapper.selectList(new LambdaQueryWrapper<WfForumThread>()
                        .ne(WfForumThread::getStatus, ForumThreadStatus.DRAFT)
                        .ge(WfForumThread::getCreateTime, startAt)
                        .lt(WfForumThread::getCreateTime, endAt)),
                WfForumThread::getCreateTime
        );
        Map<LocalDate, Long> replyCountMap = aggregateByDate(
                wfForumReplyMapper.selectList(new LambdaQueryWrapper<WfForumReply>()
                        .eq(WfForumReply::getStatus, ForumReplyStatus.NORMAL)
                        .ge(WfForumReply::getCreateTime, startAt)
                        .lt(WfForumReply::getCreateTime, endAt)),
                WfForumReply::getCreateTime
        );
        Map<LocalDate, Long> lobbyMessageCountMap = aggregateByDate(
                wfLobbyMessageMapper.selectList(new LambdaQueryWrapper<WfLobbyMessage>()
                        .ge(WfLobbyMessage::getCreateTime, startAt)
                        .lt(WfLobbyMessage::getCreateTime, endAt)),
                WfLobbyMessage::getCreateTime
        );

        List<WfLoginRecord> loginRecords = wfLoginRecordMapper.selectList(new LambdaQueryWrapper<WfLoginRecord>()
                .ge(WfLoginRecord::getLoginTime, startAt)
                .lt(WfLoginRecord::getLoginTime, endAt));
        Map<LocalDate, Long> loginSuccessMap = aggregateByDate(
                loginRecords.stream()
                        .filter(record -> LoginResult.SUCCESS.equals(record.getLoginResult()))
                        .collect(Collectors.toList()),
                WfLoginRecord::getLoginTime
        );
        Map<LocalDate, Long> loginFailMap = aggregateByDate(
                loginRecords.stream()
                        .filter(record -> LoginResult.FAIL.equals(record.getLoginResult()))
                        .collect(Collectors.toList()),
                WfLoginRecord::getLoginTime
        );

        List<String> dateLabels = new ArrayList<>(trendDays);
        List<Long> registerCounts = new ArrayList<>(trendDays);
        List<Long> threadCounts = new ArrayList<>(trendDays);
        List<Long> replyCounts = new ArrayList<>(trendDays);
        List<Long> lobbyMessageCounts = new ArrayList<>(trendDays);
        List<Long> loginSuccessCounts = new ArrayList<>(trendDays);
        List<Long> loginFailCounts = new ArrayList<>(trendDays);

        for (int i = 0; i < trendDays; i++) {
            LocalDate date = startDate.plusDays(i);
            dateLabels.add(date.format(DATE_LABEL_FORMATTER));
            registerCounts.add(registerCountMap.getOrDefault(date, 0L));
            threadCounts.add(threadCountMap.getOrDefault(date, 0L));
            replyCounts.add(replyCountMap.getOrDefault(date, 0L));
            lobbyMessageCounts.add(lobbyMessageCountMap.getOrDefault(date, 0L));
            loginSuccessCounts.add(loginSuccessMap.getOrDefault(date, 0L));
            loginFailCounts.add(loginFailMap.getOrDefault(date, 0L));
        }

        AdminDashboardTrendVO trendVO = new AdminDashboardTrendVO();
        trendVO.setDateLabels(dateLabels);
        trendVO.setRegisterCounts(registerCounts);
        trendVO.setThreadCounts(threadCounts);
        trendVO.setReplyCounts(replyCounts);
        trendVO.setLobbyMessageCounts(lobbyMessageCounts);
        trendVO.setLoginSuccessCounts(loginSuccessCounts);
        trendVO.setLoginFailCounts(loginFailCounts);
        trendVO.setLoginClientTypeDistribution(buildClientTypeDistribution(loginRecords));
        return trendVO;
    }

    private int normalizeTrendDays(int days) {
        if (days < 3 || days > 30) {
            return DEFAULT_TREND_DAYS;
        }
        return days;
    }

    private <T> Map<LocalDate, Long> aggregateByDate(List<T> records, Function<T, LocalDateTime> timeGetter) {
        Map<LocalDate, Long> countMap = new LinkedHashMap<>();
        for (T record : records) {
            LocalDateTime time = timeGetter.apply(record);
            if (time == null) {
                continue;
            }
            LocalDate date = time.toLocalDate();
            countMap.put(date, countMap.getOrDefault(date, 0L) + 1L);
        }
        return countMap;
    }

    private List<AdminDashboardClientTypeVO> buildClientTypeDistribution(List<WfLoginRecord> loginRecords) {
        Map<String, Long> countMap = new LinkedHashMap<>();
        for (WfLoginRecord record : loginRecords) {
            String clientType = record.getClientType();
            if (!StringUtils.hasText(clientType)) {
                clientType = "UNKNOWN";
            }
            countMap.put(clientType, countMap.getOrDefault(clientType, 0L) + 1L);
        }
        return countMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(6)
                .map(entry -> {
                    AdminDashboardClientTypeVO vo = new AdminDashboardClientTypeVO();
                    vo.setClientType(entry.getKey());
                    vo.setCount(entry.getValue());
                    return vo;
                })
                .collect(Collectors.toList());
    }
}

