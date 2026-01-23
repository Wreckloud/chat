package com.wreckloud.wolfchat.follow.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.enums.FollowStatus;
import com.wreckloud.wolfchat.follow.api.vo.FollowUserVO;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import com.wreckloud.wolfchat.follow.infra.mapper.WfFollowMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @Description 关注关系服务
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Service
@RequiredArgsConstructor
public class FollowService {
    private final WfFollowMapper wfFollowMapper;
    private final WfUserMapper wfUserMapper;

    /**
     * 关注行者
     */
    @Transactional(rollbackFor = Exception.class)
    public void follow(Long followerId, Long followeeId) {
        if (followerId.equals(followeeId)) {
            throw new BaseException(ErrorCode.FOLLOW_SELF);
        }

        WfUser target = wfUserMapper.selectById(followeeId);
        if (target == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, followerId)
                .eq(WfFollow::getFolloweeId, followeeId);
        WfFollow follow = wfFollowMapper.selectOne(queryWrapper);

        if (follow != null) {
            if (FollowStatus.FOLLOWING.equals(follow.getStatus())) {
                throw new BaseException(ErrorCode.FOLLOW_ALREADY);
            }
            LambdaUpdateWrapper<WfFollow> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WfFollow::getId, follow.getId())
                    .set(WfFollow::getStatus, FollowStatus.FOLLOWING);
            wfFollowMapper.update(null, updateWrapper);
            return;
        }

        WfFollow newFollow = new WfFollow();
        newFollow.setFollowerId(followerId);
        newFollow.setFolloweeId(followeeId);
        newFollow.setStatus(FollowStatus.FOLLOWING);
        wfFollowMapper.insert(newFollow);
    }

    /**
     * 取消关注
     */
    @Transactional(rollbackFor = Exception.class)
    public void unfollow(Long followerId, Long followeeId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, followerId)
                .eq(WfFollow::getFolloweeId, followeeId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING);
        WfFollow follow = wfFollowMapper.selectOne(queryWrapper);
        if (follow == null) {
            throw new BaseException(ErrorCode.FOLLOW_NOT_FOUND);
        }

        LambdaUpdateWrapper<WfFollow> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfFollow::getId, follow.getId())
                .set(WfFollow::getStatus, FollowStatus.UNFOLLOWED);
        wfFollowMapper.update(null, updateWrapper);
    }

    /**
     * 获取关注列表
     */
    public List<FollowUserVO> getFollowing(Long userId) {
        List<WfFollow> following = getFollowingRecords(userId);
        Set<Long> followerSet = getFollowerIds(userId);
        List<Long> userIds = following.stream()
                .map(WfFollow::getFolloweeId)
                .collect(Collectors.toList());
        return buildFollowUsers(userIds, followerSet);
    }

    /**
     * 获取粉丝列表
     */
    public List<FollowUserVO> getFollowers(Long userId) {
        List<WfFollow> followers = getFollowerRecords(userId);
        Set<Long> followingSet = getFollowingIds(userId);
        List<Long> userIds = followers.stream()
                .map(WfFollow::getFollowerId)
                .collect(Collectors.toList());
        return buildFollowUsers(userIds, followingSet);
    }

    /**
     * 获取互关列表
     */
    public List<FollowUserVO> getMutual(Long userId) {
        Set<Long> followingSet = getFollowingIds(userId);
        Set<Long> followerSet = getFollowerIds(userId);

        followingSet.retainAll(followerSet);
        if (followingSet.isEmpty()) {
            return new ArrayList<>();
        }

        List<WfUser> users = wfUserMapper.selectBatchIds(followingSet);
        Map<Long, WfUser> userMap = users.stream()
                .collect(Collectors.toMap(WfUser::getId, user -> user));

        List<FollowUserVO> result = new ArrayList<>();
        for (Long id : followingSet) {
            WfUser user = userMap.get(id);
            if (user != null) {
                result.add(toFollowUserVO(user, true));
            }
        }
        return result;
    }

    private List<WfFollow> getFollowingRecords(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFollowerId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .orderByDesc(WfFollow::getCreateTime);
        return wfFollowMapper.selectList(queryWrapper);
    }

    private List<WfFollow> getFollowerRecords(Long userId) {
        LambdaQueryWrapper<WfFollow> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfFollow::getFolloweeId, userId)
                .eq(WfFollow::getStatus, FollowStatus.FOLLOWING)
                .orderByDesc(WfFollow::getCreateTime);
        return wfFollowMapper.selectList(queryWrapper);
    }

    private Set<Long> getFollowingIds(Long userId) {
        return getFollowingRecords(userId).stream()
                .map(WfFollow::getFolloweeId)
                .collect(Collectors.toSet());
    }

    private Set<Long> getFollowerIds(Long userId) {
        return getFollowerRecords(userId).stream()
                .map(WfFollow::getFollowerId)
                .collect(Collectors.toSet());
    }

    private List<FollowUserVO> buildFollowUsers(List<Long> userIds, Set<Long> mutualBase) {
        if (userIds.isEmpty()) {
            return new ArrayList<>();
        }

        List<WfUser> users = wfUserMapper.selectBatchIds(userIds);
        Map<Long, WfUser> userMap = new HashMap<>();
        for (WfUser user : users) {
            userMap.put(user.getId(), user);
        }

        List<FollowUserVO> result = new ArrayList<>();
        for (Long id : userIds) {
            WfUser user = userMap.get(id);
            if (user != null) {
                boolean mutual = mutualBase.contains(user.getId());
                result.add(toFollowUserVO(user, mutual));
            }
        }
        return result;
    }

    private FollowUserVO toFollowUserVO(WfUser user, boolean mutual) {
        FollowUserVO vo = new FollowUserVO();
        vo.setUserId(user.getId());
        vo.setWolfNo(user.getWolfNo());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus() != null ? user.getStatus().getValue() : null);
        vo.setMutual(mutual);
        return vo;
    }
}
