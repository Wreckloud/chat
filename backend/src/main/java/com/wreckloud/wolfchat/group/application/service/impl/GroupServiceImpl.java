package com.wreckloud.wolfchat.group.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.group.api.dto.GroupCreateDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupUpdateDTO;
import com.wreckloud.wolfchat.group.api.vo.GroupDetailVO;
import com.wreckloud.wolfchat.group.api.vo.GroupMemberVO;
import com.wreckloud.wolfchat.group.api.vo.GroupVO;
import com.wreckloud.wolfchat.group.application.service.GroupService;
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
 * @Description 群组服务实现类
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Slf4j
@Service
public class GroupServiceImpl implements GroupService {

    @Autowired
    private WfGroupMapper groupMapper;

    @Autowired
    private WfGroupMemberMapper groupMemberMapper;

    @Autowired
    private WfUserMapper userMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public GroupVO createGroup(GroupCreateDTO dto, Long creatorId) {
        log.info("创建群组，创建者ID: {}, 群名称: {}, 成员数: {}", creatorId, dto.getGroupName(), dto.getMemberIds().size());

        // 1. 校验创建者是否存在
        WfUser creator = userMapper.selectById(creatorId);
        if (creator == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }

        // 2. 校验成员数量是否超过上限
        int totalMembers = dto.getMemberIds().size() + 1; // +1 是创建者自己
        if (totalMembers > dto.getMaxMembers()) {
            throw new BaseException(ErrorCode.GROUP_MEMBER_FULL);
        }

        // 3. 创建群组
        WfGroup group = new WfGroup();
        group.setGroupName(dto.getGroupName());
        group.setGroupAvatar(dto.getGroupAvatar());
        group.setGroupIntro(dto.getGroupIntro());
        group.setOwnerId(creatorId);
        group.setMemberCount(totalMembers);
        group.setMaxMembers(dto.getMaxMembers());
        group.setIsAllMuted(false);
        group.setIsNeedApproval(false);
        group.setStatus(GroupStatus.NORMAL.getCode());
        groupMapper.insert(group);

        log.info("群组创建成功，群组ID: {}", group.getId());

        // 4. 添加群主成员记录
        WfGroupMember ownerMember = new WfGroupMember();
        ownerMember.setGroupId(group.getId());
        ownerMember.setUserId(creatorId);
        ownerMember.setRole(GroupMemberRole.OWNER.getCode());
        ownerMember.setIsMuted(false);
        groupMemberMapper.insert(ownerMember);

        // 5. 批量添加其他成员
        for (Long memberId : dto.getMemberIds()) {
            // 校验成员是否存在
            WfUser member = userMapper.selectById(memberId);
            if (member == null) {
                log.warn("成员不存在，用户ID: {}", memberId);
                continue;
            }

            WfGroupMember groupMember = new WfGroupMember();
            groupMember.setGroupId(group.getId());
            groupMember.setUserId(memberId);
            groupMember.setRole(GroupMemberRole.MEMBER.getCode());
            groupMember.setIsMuted(false);
            groupMemberMapper.insert(groupMember);
        }

        log.info("群成员添加完成，共 {} 人", totalMembers);

        // 6. 转换为VO返回
        return convertToGroupVO(group, creator, GroupMemberRole.OWNER);
    }

    @Override
    public GroupDetailVO getGroupDetail(Long groupId, Long userId) {
        log.info("查询群详情，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组信息
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }
        if (GroupStatus.DISBANDED.getCode().equals(group.getStatus())) {
            throw new BaseException(ErrorCode.GROUP_DISBANDED);
        }

        // 2. 查询当前用户在群中的角色
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

        // 5. 查询群主信息
        WfUser owner = userMap.get(group.getOwnerId());

        // 6. 转换为VO
        GroupDetailVO vo = new GroupDetailVO();
        vo.setGroupId(group.getId());
        vo.setGroupName(group.getGroupName());
        vo.setGroupAvatar(group.getGroupAvatar());
        vo.setGroupIntro(group.getGroupIntro());
        vo.setOwnerId(group.getOwnerId());
        vo.setOwnerWfNo(owner != null ? owner.getWfNo() : null);
        vo.setOwnerName(owner != null ? owner.getUsername() : null);
        vo.setMemberCount(group.getMemberCount());
        vo.setMaxMembers(group.getMaxMembers());
        vo.setIsAllMuted(group.getIsAllMuted());
        vo.setIsNeedApproval(group.getIsNeedApproval());
        vo.setMyRole(GroupMemberRole.getByCode(myMembership.getRole()).name());
        vo.setCreateTime(group.getCreateTime());

        // 7. 转换成员列表
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
        vo.setMembers(memberVOs);

        return vo;
    }

    @Override
    public List<GroupVO> getMyGroups(Long userId) {
        log.info("查询我的群组列表，用户ID: {}", userId);

        // 1. 查询用户加入的所有群组
        List<WfGroupMember> myMemberships = groupMemberMapper.selectList(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getUserId, userId)
                        .eq(WfGroupMember::getIsDeleted, false)
        );

        if (myMemberships.isEmpty()) {
            return new ArrayList<>();
        }

        // 2. 查询群组信息
        List<Long> groupIds = myMemberships.stream().map(WfGroupMember::getGroupId).collect(Collectors.toList());
        List<WfGroup> groups = groupMapper.selectBatchIds(groupIds);
        groups = groups.stream()
                .filter(g -> !g.getIsDeleted() && GroupStatus.NORMAL.getCode().equals(g.getStatus()))
                .collect(Collectors.toList());

        // 3. 查询所有群主信息
        List<Long> ownerIds = groups.stream().map(WfGroup::getOwnerId).distinct().collect(Collectors.toList());
        List<WfUser> owners = userMapper.selectBatchIds(ownerIds);
        Map<Long, WfUser> ownerMap = owners.stream().collect(Collectors.toMap(WfUser::getId, o -> o));

        // 4. 构建我的角色映射
        Map<Long, Integer> myRoleMap = myMemberships.stream()
                .collect(Collectors.toMap(WfGroupMember::getGroupId, WfGroupMember::getRole));

        // 5. 转换为VO
        return groups.stream()
                .map(group -> {
                    WfUser owner = ownerMap.get(group.getOwnerId());
                    Integer myRole = myRoleMap.get(group.getId());
                    return convertToGroupVO(group, owner, GroupMemberRole.getByCode(myRole));
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateGroup(Long groupId, GroupUpdateDTO dto, Long userId) {
        log.info("修改群信息，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验权限：只有群主可以修改群信息
        if (!group.getOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 更新群信息
        if (dto.getGroupName() != null) {
            group.setGroupName(dto.getGroupName());
        }
        if (dto.getGroupAvatar() != null) {
            group.setGroupAvatar(dto.getGroupAvatar());
        }
        if (dto.getGroupIntro() != null) {
            group.setGroupIntro(dto.getGroupIntro());
        }

        groupMapper.updateById(group);
        log.info("群信息修改成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disbandGroup(Long groupId, Long userId) {
        log.info("解散群组，群组ID: {}, 用户ID: {}", groupId, userId);

        // 1. 查询群组
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null || group.getIsDeleted()) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }

        // 2. 校验权限：只有群主可以解散群组
        if (!group.getOwnerId().equals(userId)) {
            throw new BaseException(ErrorCode.NO_GROUP_PERMISSION);
        }

        // 3. 解散群组（软删除）
        group.setStatus(GroupStatus.DISBANDED.getCode());
        group.setIsDeleted(true);
        groupMapper.updateById(group);

        // 4. 删除所有群成员记录（软删除）
        groupMemberMapper.delete(
                new LambdaQueryWrapper<WfGroupMember>()
                        .eq(WfGroupMember::getGroupId, groupId)
        );

        log.info("群组解散成功");
    }

    /**
     * 转换为GroupVO
     */
    private GroupVO convertToGroupVO(WfGroup group, WfUser owner, GroupMemberRole myRole) {
        GroupVO vo = new GroupVO();
        vo.setGroupId(group.getId());
        vo.setGroupName(group.getGroupName());
        vo.setGroupAvatar(group.getGroupAvatar());
        vo.setGroupIntro(group.getGroupIntro());
        vo.setOwnerId(group.getOwnerId());
        vo.setOwnerWfNo(owner != null ? owner.getWfNo() : null);
        vo.setOwnerName(owner != null ? owner.getUsername() : null);
        vo.setMemberCount(group.getMemberCount());
        vo.setMaxMembers(group.getMaxMembers());
        vo.setMyRole(myRole != null ? myRole.name() : null);
        vo.setCreateTime(group.getCreateTime());
        return vo;
    }
}

