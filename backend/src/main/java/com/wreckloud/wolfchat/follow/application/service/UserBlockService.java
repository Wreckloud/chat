package com.wreckloud.wolfchat.follow.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.application.service.UserService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.follow.domain.entity.WfUserBlock;
import com.wreckloud.wolfchat.follow.domain.enums.UserBlockStatus;
import com.wreckloud.wolfchat.follow.infra.mapper.WfUserBlockMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户拉黑服务
 */
@Service
@RequiredArgsConstructor
public class UserBlockService {
    private final WfUserBlockMapper wfUserBlockMapper;
    private final UserService userService;

    @Transactional(rollbackFor = Exception.class)
    public void blockUser(Long blockerId, Long blockedId) {
        validateNotSelf(blockerId, blockedId);
        userService.getByIdOrThrow(blockedId);

        WfUserBlock relation = findRelation(blockerId, blockedId);
        if (relation != null) {
            if (UserBlockStatus.BLOCKED.equals(relation.getStatus())) {
                return;
            }
            updateStatusById(relation.getId(), UserBlockStatus.BLOCKED);
            return;
        }

        WfUserBlock newRelation = new WfUserBlock();
        newRelation.setBlockerId(blockerId);
        newRelation.setBlockedId(blockedId);
        newRelation.setStatus(UserBlockStatus.BLOCKED);
        try {
            int insertRows = wfUserBlockMapper.insert(newRelation);
            if (insertRows != 1) {
                throw new BaseException(ErrorCode.DATABASE_ERROR);
            }
        } catch (DuplicateKeyException ex) {
            WfUserBlock existing = findRelation(blockerId, blockedId);
            if (existing != null && !UserBlockStatus.BLOCKED.equals(existing.getStatus())) {
                updateStatusById(existing.getId(), UserBlockStatus.BLOCKED);
                return;
            }
            throw ex;
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public void unblockUser(Long blockerId, Long blockedId) {
        validateNotSelf(blockerId, blockedId);
        WfUserBlock relation = findRelation(blockerId, blockedId);
        if (relation == null || !UserBlockStatus.BLOCKED.equals(relation.getStatus())) {
            return;
        }
        updateStatusById(relation.getId(), UserBlockStatus.UNBLOCKED);
    }

    public boolean isBlocked(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId <= 0L || blockedId <= 0L) {
            return false;
        }
        LambdaQueryWrapper<WfUserBlock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserBlock::getBlockerId, blockerId)
                .eq(WfUserBlock::getBlockedId, blockedId)
                .eq(WfUserBlock::getStatus, UserBlockStatus.BLOCKED)
                .last("LIMIT 1");
        return wfUserBlockMapper.selectCount(queryWrapper) > 0;
    }

    private WfUserBlock findRelation(Long blockerId, Long blockedId) {
        LambdaQueryWrapper<WfUserBlock> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfUserBlock::getBlockerId, blockerId)
                .eq(WfUserBlock::getBlockedId, blockedId)
                .last("LIMIT 1");
        return wfUserBlockMapper.selectOne(queryWrapper);
    }

    private void updateStatusById(Long id, UserBlockStatus status) {
        LambdaUpdateWrapper<WfUserBlock> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfUserBlock::getId, id)
                .set(WfUserBlock::getStatus, status);
        int updateRows = wfUserBlockMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }

    private void validateNotSelf(Long blockerId, Long blockedId) {
        if (blockerId == null || blockedId == null || blockerId <= 0L || blockedId <= 0L) {
            throw new BaseException(ErrorCode.PARAM_ERROR);
        }
        if (blockerId.equals(blockedId)) {
            throw new BaseException(ErrorCode.PARAM_ERROR, "不能拉黑自己");
        }
    }
}

