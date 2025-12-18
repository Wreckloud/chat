package com.wreckloud.wolfchat.admin.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.admin.api.vo.AdminStatisticsVO;
import com.wreckloud.wolfchat.admin.application.service.AdminStatisticsService;
import com.wreckloud.wolfchat.group.domain.entity.WfGroup;
import com.wreckloud.wolfchat.group.domain.enums.GroupStatus;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * @Description 管理员-统计服务实现类
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminStatisticsServiceImpl implements AdminStatisticsService {
    
    private final WfUserMapper userMapper;
    private final WfGroupMapper groupMapper;
    
    @Override
    public AdminStatisticsVO getStatistics() {
        log.info("管理员获取统计数据");
        
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.toLocalDate().atStartOfDay();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        
        // 用户统计
        Long totalUsers = userMapper.selectCount(null);
        
        Long todayNewUsers = userMapper.selectCount(
                new LambdaQueryWrapper<WfUser>()
                        .ge(WfUser::getCreateTime, todayStart)
        );
        
        Long activeUsers = userMapper.selectCount(
                new LambdaQueryWrapper<WfUser>()
                        .ge(WfUser::getLastLoginTime, sevenDaysAgo)
        );
        
        Long disabledUsers = userMapper.selectCount(
                new LambdaQueryWrapper<WfUser>()
                        .eq(WfUser::getStatus, 2)  // 状态2=禁用
        );
        
        // 群组统计
        Long totalGroups = groupMapper.selectCount(null);
        
        Long todayNewGroups = groupMapper.selectCount(
                new LambdaQueryWrapper<WfGroup>()
                        .ge(WfGroup::getCreateTime, todayStart)
        );
        
        // TODO:活跃群组数（7天内有消息）- 暂时用创建时间代替，后续可以关联消息表
        Long activeGroups = groupMapper.selectCount(
                new LambdaQueryWrapper<WfGroup>()
                        .ge(WfGroup::getUpdateTime, sevenDaysAgo)
                        .eq(WfGroup::getStatus, GroupStatus.NORMAL.getCode())
        );
        
        Long disbandedGroups = groupMapper.selectCount(
                new LambdaQueryWrapper<WfGroup>()
                        .eq(WfGroup::getStatus, GroupStatus.DISBANDED.getCode())
        );
        
        return AdminStatisticsVO.builder()
                .totalUsers(totalUsers)
                .todayNewUsers(todayNewUsers)
                .activeUsers(activeUsers)
                .disabledUsers(disabledUsers)
                .totalGroups(totalGroups)
                .todayNewGroups(todayNewGroups)
                .activeGroups(activeGroups)
                .disbandedGroups(disbandedGroups)
                .build();
    }
}

