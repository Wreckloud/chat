package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.account.infra.mapper.WfNoPoolMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;

/**
 * @Description 狼藉号分配服务，实现批量预生成 + 用完补充策略
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WolfNoService {
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

        // 2. 从号码池随机获取一个 UNUSED 号码
        WfNoPool noPool = getRandomUnusedWolfNo();
        if (noPool == null) {
            throw new BaseException(ErrorCode.WOLF_NO_POOL_EMPTY);
        }

        // 3. 更新状态为 USED 并绑定 user_id
        LambdaUpdateWrapper<WfNoPool> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(WfNoPool::getId, noPool.getId())
                .eq(WfNoPool::getStatus, "UNUSED")
                .set(WfNoPool::getStatus, "USED")
                .set(WfNoPool::getUserId, userId);

        int updateCount = wfNoPoolMapper.update(null, updateWrapper);
        if (updateCount == 0) {
            // 并发冲突，重试
            log.warn("狼藉号分配冲突，重试: wolfNo={}", noPool.getWolfNo());
            return allocateWolfNo(userId);
        }

        log.info("狼藉号分配成功: wolfNo={}, userId={}", noPool.getWolfNo(), userId);
        return noPool.getWolfNo();
    }

    /**
     * 从号码池随机获取一个 UNUSED 号码
     */
    private WfNoPool getRandomUnusedWolfNo() {
        // 查询所有 UNUSED 状态的号码
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, "UNUSED");
        List<WfNoPool> unusedList = wfNoPoolMapper.selectList(queryWrapper);

        if (unusedList.isEmpty()) {
            return null;
        }

        // 随机选择一个
        Random random = new Random();
        int index = random.nextInt(unusedList.size());
        return unusedList.get(index);
    }

    /**
     * 检查号码池并补充
     * 当 UNUSED 数量低于阈值（10个）时，自动补充50个新号码
     */
    private void checkAndReplenishPool() {
        LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfNoPool::getStatus, "UNUSED");
        long unusedCount = wfNoPoolMapper.selectCount(queryWrapper);

        if (unusedCount < 10) {
            log.info("号码池 UNUSED 数量不足，开始补充: current={}", unusedCount);
            replenishPool(50);
        }
    }

    /**
     * 补充号码池
     * 生成指定数量的新号码（10位，首位1-9，避免前导0）
     *
     * @param count 补充数量
     */
    private void replenishPool(int count) {
        Random random = new Random();
        int added = 0;
        int maxRetries = count * 10; // 最大重试次数
        int retries = 0;

        while (added < count && retries < maxRetries) {
            // 生成10位随机号码（范围：1000000000-9999999999，首位1-9）
            long min = 1000000000L;
            long max = 9999999999L;
            long randomNo = min + (long) (random.nextDouble() * (max - min + 1));
            String wolfNo = String.valueOf(randomNo);

            // 检查是否已存在
            LambdaQueryWrapper<WfNoPool> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(WfNoPool::getWolfNo, wolfNo);
            long existCount = wfNoPoolMapper.selectCount(queryWrapper);

            if (existCount == 0) {
                // 插入新号码
                WfNoPool newNo = new WfNoPool();
                newNo.setWolfNo(wolfNo);
                newNo.setStatus("UNUSED");
                wfNoPoolMapper.insert(newNo);
                added++;
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
        wfNoPoolMapper.update(null, updateWrapper);
    }
}

