package com.wreckloud.wolfchat.group.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.group.api.vo.GroupMemberVO;
import com.wreckloud.wolfchat.group.application.service.GroupMemberService;
import com.wreckloud.wolfchat.group.domain.entity.WfGroup;
import com.wreckloud.wolfchat.group.domain.entity.WfGroupMember;
import com.wreckloud.wolfchat.group.domain.enums.GroupMemberRole;
import com.wreckloud.wolfchat.group.domain.enums.GroupStatus;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMapper;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMemberMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 群成员服务实现类
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Slf4j
@Service
public class GroupMemberServiceImpl implements GroupMemberService {

    @Autowired
    private WfGroupMapper groupMapper;

    @Autowired
    private WfGroupMemberMapper groupMemberMapper;

    @Autowired
    private WfUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void inviteMembers(Long groupId, List<Long> userIds, Long inviterId) {
        log.info("邀请成员入群，群组ID: {}, 邀请人ID: {}, 被邀请人数: {}", groupId, inviterId, userIds.size());

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (GroupStatus.DISBANDED.getCode().equals(group.getStatus())) {
            throw new BaseException(ErrorCode.GROUP_DISBANDED);
        }

        // 2. 校验邀请人是否在群内
        WfGroupMember inviterMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, inviterId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (inviterMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 检查群人数是否已满
        int currentMemberCount = groupMemberMapper.selectCount(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getIsDeleted, false)
        ).intValue();

        if (currentMemberCount + userIds.size() > group.getMaxMembers()) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_FULL);
        }

        // 4. 批量添加成员
        int successCount = 0;
        for (Long userId : userIds) {
            // 检查用户是否存在
            WfUser user = userMapper.selectById(userId);
            if (user == null) {
                log.warn("用户不存在，用户ID: {}", userId);
                continue;
            }

            // 检查是否已在群内
            WfGroupMember existingMember = groupMemberMapper.selectOne(
                    new LambdaQueryWrapper<WfGroupMember>()
                            .eq(WfGroupMember::getGroupId, groupId)
                            .eq(WfGroupMember::getUserId, userId)
                            .eq(WfGroupMember::getIsDeleted, false)
            );
            if (existingMember != null) {
                log.warn("用户已在群内，用户ID: {}", userId);
                continue;
            }

            // 添加群成员记录
            WfGroupMember groupMember = new WfGroupMember();
            groupMember.setGroupId(groupId);
            groupMember.setUserId(userId);
            groupMember.setRole(GroupMemberRole.MEMBER.getCode());
            groupMember.setIsMuted(false);
            groupMemberMapper.insert(groupMember);
            successCount++;
        }

        // 5. 更新群成员数
        if (successCount > 0) {
            group.setMemberCount(group.getMemberCount() + successCount);
            groupMapper.updateById(group);
        }

        log.info("成员邀请完成，成功添加 {} 人", successCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void kickMember(Long groupId, Long targetUserId, Long operatorId) {
        log.info("踢出成员，群组ID: {}, 被踢出用户ID: {}, 操作人ID: {}", groupId, targetUserId, operatorId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 查询操作人角色
        WfGroupMember operatorMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, operatorId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (operatorMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 查询被踢出用户
        WfGroupMember targetMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, targetUserId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (targetMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 4. 权限校验
        Integer operatorRole = operatorMember.getRole();
        Integer targetRole = targetMember.getRole();

        // 不能踢出群主
        if (GroupMemberRole.OWNER.getCode().equals(targetRole)) {
            throw new BaseException(ErrorCode.CANNOT_KICK_OWNER);
        }

        // 群主可以踢出任何人
        // 管理员只能踢出普通成员，不能踢出其他管理员和群主
        if (GroupMemberRole.MEMBER.getCode().equals(operatorRole)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }
        if (GroupMemberRole.ADMIN.getCode().equals(operatorRole) && 
            !GroupMemberRole.MEMBER.getCode().equals(targetRole)) {
            throw new BaseException(ErrorCode.CANNOT_KICK_ADMIN);
        }

        // 5. 删除成员记录（软删除）
        targetMember.setIsDeleted(true);
        groupMemberMapper.updateById(targetMember);

        // 6. 更新群成员数
        group.setMemberCount(group.getMemberCount() - 1);
        groupMapper.updateById(group);

        log.info("成员踢出成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void quitGroup(Long groupId, Long userId) {
        log.info("退出群组，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 查询成员信息
        WfGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (member == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 群主不能退出，只能转让或解散
        if (GroupMemberRole.OWNER.getCode().equals(member.getRole())) {
            throw new BaseException(ErrorCode.GROUP_OWNER_CANNOT_QUIT);
        }

        // 4. 删除成员记录（软删除）
        member.setIsDeleted(true);
        groupMemberMapper.updateById(member);

        // 5. 更新群成员数
        group.setMemberCount(group.getMemberCount() - 1);
        groupMapper.updateById(group);

        log.info("退出群组成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void setAdmin(Long groupId, Long targetUserId, Boolean isAdmin, Long operatorId) {
        log.info("设置管理员，群组ID: {}, 目标用户ID: {}, 是否设置: {}, 操作人ID: {}", 
                groupId, targetUserId, isAdmin, operatorId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验操作人是否是群主
        if (!group.getOwnerId().equals(operatorId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 查询目标用户
        WfGroupMember targetMember = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, targetUserId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (targetMember == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 4. 不能修改群主角色
        if (GroupMemberRole.OWNER.getCode().equals(targetMember.getRole())) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 5. 设置角色
        if (isAdmin) {
            // 设置为管理员
            targetMember.setRole(GroupMemberRole.ADMIN.getCode());
            log.info("用户 {} 被设置为管理员", targetUserId);
        } else {
            // 取消管理员，恢复为普通成员
            targetMember.setRole(GroupMemberRole.MEMBER.getCode());
            log.info("用户 {} 的管理员权限被取消", targetUserId);
        }
        groupMemberMapper.updateById(targetMember);

        log.info("管理员设置成功");
    }

    @Override
    public List<GroupMemberVO> getMembers(Long groupId, Long userId) {
        log.info("查询群成员列表，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验用户是否在群内
        WfGroupMember myMembership = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (myMembership == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 查询所有群成员
        List<WfGroupMember> members = groupMemberMapper.selectList(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getIsDeleted, false)
                        .orderByAsc(WfGroupMember::getRole)
                        .orderByAsc(WfGroupMember::getJoinTime)
        );

        // 4. 查询所有成员的用户信息
        List<Long> userIds = members.stream().map(WfGroupMember::getUserId).collect(Collectors.toList());
        List<WfUser> users = userMapper.selectBatchIds(userIds);
        Map<Long, WfUser> userMap = users.stream().collect(Collectors.toMap(WfUser::getId, u -> u));

        // 5. 转换为VO
        List<GroupMemberVO> memberVOs = new ArrayList<>();
        for (WfGroupMember member : members) {
            WfUser user = userMap.get(member.getUserId());
            if (user == null) continue;

            GroupMemberVO memberVO = new GroupMemberVO();
            memberVO.setUserId(user.getId());
            memberVO.setWfNo(user.getWfNo());
            memberVO.setUsername(user.getUsername());
            memberVO.setAvatar(user.getAvatar());
            memberVO.setGroupNickname(member.getGroupNickname());
            memberVO.setRole(GroupMemberRole.getByCode(member.getRole()).name());
            memberVO.setIsMuted(member.getIsMuted());
            memberVO.setJoinTime(member.getJoinTime());
            memberVOs.add(memberVO);
        }

        return memberVOs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateNickname(Long groupId, Long userId, String nickname) {
        log.info("修改群昵称，群组ID: {}, 用户ID: {}, 新昵称: {}", groupId, userId, nickname);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 查询成员信息
        WfGroupMember member = groupMemberMapper.selectOne(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );
        if (member == null) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_NOT_FOUND);
        }

        // 3. 更新群昵称
        member.setGroupNickname(nickname);
        groupMemberMapper.updateById(member);

        log.info("群昵称修改成功");
    }
}

