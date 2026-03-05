package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.account.infra.mapper.WfNoPoolMapper;
import com.wreckloud.wolfchat.account.domain.enums.NoPoolStatus;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.concurrent.ThreadLocalRandom;

/**
 * @Description 狼藉号分配服务，实现批量预生成 + 用完补充策略
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WolfNoService {
    /**
     * 号码池补充阈值：当 UNUSED 数量低于此值时触发补充
     */
    private static final int REPLENISH_THRESHOLD = 10;

    /**
     * 每次补充的号码数量
     */
    private static final int REPLENISH_COUNT = 50;

    /**
     * 号码生成重试倍数：最大重试次数 = 补充数量 × 此倍数
     */
    private static final int MAX_RETRY_MULTIPLIER = 10;

    /**
     * 分配重试次数：并发冲突时最多重试次数
     */
    private static final int ALLOCATE_MAX_RETRY = 5;

    private final WfNoPoolMapper wfNoPoolMapper;

    /**
     * 分配一个狼藉号
     * 从号码池随机获取 UNUSED 号码，分配后更新状态为 USED 并绑定 user_id
     *
     * @param userId 行者ID
     * @return 分配的狼藉号
     */
    @Transactional(rollbackFor = Exception.class)
    public String allocateWolfNo(Long userId) {
        // 1. 检查号码池中 UNUSED 数量，低于阈值时自动补充
        checkAndReplenishPool();

        // 2. 并发冲突时进行有限重试（避免递归调用）
        for (int retry = 1; retry <= ALLOCATE_MAX_RETRY; retry++) {
            WfNoPool noPool = getRandomUnusedWolfNo();
            if (noPool == null) {
                throw new BaseException(ErrorCode.WOLF_NO_POOL_EMPTY);
            }

            LambdaUpdateWrapper<WfNoPool> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.eq(WfNoPool::getId, noPool.getId())
                    .eq(WfNoPool::getStatus, NoPoolStatus.UNUSED)
                    .set(WfNoPool::getStatus, NoPoolStatus.USED)
                    .set(WfNoPool::getUserId, userId);

            int updateCount = wfNoPoolMapper.update(null, updateWrapper);
            if (updateCount == 1) {
                log.info("狼藉号分配成功: wolfNo={}, userId={}", noPool.getWolfNo(), userId);
                return noPool.getWolfNo();
            }

            log.warn("狼藉号分配冲突，重试: wolfNo={}, retry={}/{}", noPool.getWolfNo(), retry, ALLOCATE_MAX_RETRY);
        }

        throw new BaseException(ErrorCode.WOLF_NO_ALLOCATE_FAILED);
    }

    /**
     * 从号码池随机获取一个 UNUSED 号码
     */
    private WfNoPool getRandomUnusedWolfNo() {
        Long minUnusedId = getUnusedBoundaryId(true);
        if (minUnusedId == null) {
            return null;
        }
        Long maxUnusedId = getUnusedBoundaryId(false);
        if (maxUnusedId == null) {
            return null;
        }

        long randomStartId = ThreadLocalRandom.current().nextLong(minUnusedId, maxUnusedId + 1);
        WfNoPool candidate = getFirstUnusedAfterOrEqual(randomStartId);
        if (candidate != null) {
            return candidate;
        }
        return getFirstUnusedBefore(randomStartId);
    }

    private Long getUnusedBoundaryId(boolean min) {
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, NoPoolStatus.UNUSED);
        if (min) {
            queryWrapper.orderByAsc(WfNoPool::getId);
        } else {
            queryWrapper.orderByDesc(WfNoPool::getId);
        }
        queryWrapper.last("LIMIT 1");
        WfNoPool target = wfNoPoolMapper.selectOne(queryWrapper);
        if (target == null) {
            return null;
        }
        return target.getId();
    }

    private WfNoPool getFirstUnusedAfterOrEqual(Long startId) {
        if (startId == null) {
            return null;
        }
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, NoPoolStatus.UNUSED)
                .ge(WfNoPool::getId, startId)
                .orderByAsc(WfNoPool::getId)
                .last("LIMIT 1");
        return wfNoPoolMapper.selectOne(queryWrapper);
    }

    private WfNoPool getFirstUnusedBefore(Long startId) {
        if (startId == null) {
            return null;
        }
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, NoPoolStatus.UNUSED)
                .lt(WfNoPool::getId, startId)
                .orderByDesc(WfNoPool::getId)
                .last("LIMIT 1");
        return wfNoPoolMapper.selectOne(queryWrapper);
    }

    /**
     * 检查号码池并补充
     * 当 UNUSED 数量低于阈值时，自动补充新号码
     */
    private void checkAndReplenishPool() {
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, NoPoolStatus.UNUSED);
        long unusedCount = wfNoPoolMapper.selectCount(queryWrapper);

        if (unusedCount < REPLENISH_THRESHOLD) {
            log.info("号码池 UNUSED 数量不足，开始补充: current={}, threshold={}", unusedCount, REPLENISH_THRESHOLD);
            replenishPool(REPLENISH_COUNT);
        }
    }

    /**
     * 补充号码池
     * 生成指定数量的新号码（10位，首位1-9，避免前导0）
     *
     * @param count 补充数量
     */
    private void replenishPool(int count) {
        int added = 0;
        int maxRetries = count * MAX_RETRY_MULTIPLIER; // 最大重试次数
        int retries = 0;

        while (added < count && retries < maxRetries) {
            // 生成10位随机号码（范围：1000000000-9999999999，首位1-9）
            long min = 1000000000L;
            long max = 9999999999L;
            long randomNo = ThreadLocalRandom.current().nextLong(min, max + 1);
            String wolfNo = String.valueOf(randomNo);

            // 检查是否已存在
            LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WfNoPool::getWolfNo, wolfNo);
            long existCount = wfNoPoolMapper.selectCount(queryWrapper);

            if (existCount == 0) {
                // 插入新号码
                WfNoPool newNo = new WfNoPool();
                newNo.setWolfNo(wolfNo);
                newNo.setStatus(NoPoolStatus.UNUSED);
                int insertRows = wfNoPoolMapper.insert(newNo);
                if (insertRows == 1) {
                    added++;
                }
            }

            retries++;
        }

        log.info("号码池补充完成: added={}, retries={}", added, retries);
    }

    /**
     * 根据狼藉号更新 userId
     * 用于注册时，先分配号码（userId为null），创建用户后更新userId
     *
     * @param wolfNo 狼藉号
     * @param userId 行者ID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateUserIdByWolfNo(String wolfNo, Long userId) {
        LambdaUpdateWrapper<WfNoPool> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfNoPool::getWolfNo, wolfNo)
                .set(WfNoPool::getUserId, userId);
        int updateRows = wfNoPoolMapper.update(null, updateWrapper);
        if (updateRows != 1) {
            throw new BaseException(ErrorCode.DATABASE_ERROR);
        }
    }
}

