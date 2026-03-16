package com.wreckloud.wolfchat.admin.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfLoginRecord;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.domain.enums.LoginResult;
import com.wreckloud.wolfchat.account.domain.enums.UserStatus;
import com.wreckloud.wolfchat.account.infra.mapper.WfLoginRecordMapper;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.admin.api.vo.AdminDashboardOverviewVO;
import com.wreckloud.wolfchat.chat.presence.application.service.UserPresenceService;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import com.wreckloud.wolfchat.community.domain.enums.ForumReplyStatus;
import com.wreckloud.wolfchat.community.domain.enums.ForumThreadStatus;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumReplyMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfForumThreadMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 管理端控制台服务。
 */
@Service
@RequiredArgsConstructor
public class AdminDashboardService {
    private final WfUserMapper wfUserMapper;
    private final WfForumThreadMapper wfForumThreadMapper;
    private final WfForumReplyMapper wfForumReplyMapper;
    private final WfLoginRecordMapper wfLoginRecordMapper;
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
}

