package com.wreckloud.wolfchat.account.application.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.api.dto.NoPoolQueryDTO;
import com.wreckloud.wolfchat.account.api.dto.NoPoolUpdateDTO;
import com.wreckloud.wolfchat.account.application.service.NoPoolService;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.account.infra.mapper.WfNoPoolMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.web.PageResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Random;

/**
 * @Description 号码池服务实现类
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Service
public class NoPoolServiceImpl implements NoPoolService {

    @Autowired
    private WfNoPoolMapper wfNoPoolMapper;

    /**
     * 号码最小位数（6位）
     */
    private static final long MIN_NUMBER = 100000L; // 6位数：100000

    /**
     * 号码最大位数（10位）
     */
    private static final long MAX_NUMBER = 9999999999L; // 10位数：9999999999

    /**
     * 系统自动生成的号码范围（固定10位）
     */
    private static final long AUTO_GEN_MIN = 1000000000L; // 10位数起始：1000000000
    private static final long AUTO_GEN_MAX = 9999999999L; // 10位数结束：9999999999

    /**
     * 号码池状态：0未使用 1已使用 2冻结
     */
    private static final int STATUS_UNUSED = 0;

    /**
     * 随机数生成器
     */
    private final Random random = new Random();

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int generateNumbers(int count) {
        if (count <= 0 || count > 1000) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }

        int successCount = 0;
        int maxAttempts = count * 10; // 最多尝试次数，避免无限循环
        int attempts = 0;

        while (successCount < count && attempts < maxAttempts) {
            attempts++;
            Long wfNo = generateRandomNumber();

            // 检查号码是否已存在
            LambdaQueryWrapper<WfNoPool> query = new LambdaQueryWrapper<>();
            query.eq(WfNoPool::getWfNo, wfNo);
            Long existCount = wfNoPoolMapper.selectCount(query);

            if (existCount == 0) {
                // 号码不存在，可以添加
                WfNoPool noPool = new WfNoPool();
                noPool.setWfNo(wfNo);
                noPool.setIsPretty(false); // 自动生成的都是普通号
                noPool.setStatus(STATUS_UNUSED);
                noPool.setUserId(null);
                noPool.setCreateTime(LocalDateTime.now());
                noPool.setUpdateTime(LocalDateTime.now());
                wfNoPoolMapper.insert(noPool);
                successCount++;
            }
        }

        if (successCount < count) {
            // 如果生成的号码不足，可能是号码池接近饱和
            throw new BaseException(ErrorCode.NUMBER_GENERATION_FAILED);
        }

        return successCount;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addNumber(Long wfNo, Boolean isPretty) {
        if (wfNo == null || wfNo < MIN_NUMBER || wfNo > MAX_NUMBER) {
            throw new BaseException(ErrorCode.INVALID_NUMBER);
        }

        // 检查号码是否已存在
        LambdaQueryWrapper<WfNoPool> query = new LambdaQueryWrapper<>();
        query.eq(WfNoPool::getWfNo, wfNo);
        Long existCount = wfNoPoolMapper.selectCount(query);

        if (existCount > 0) {
            throw new BaseException(ErrorCode.NUMBER_ALREADY_EXISTS);
        }

        // 添加号码
        WfNoPool noPool = new WfNoPool();
        noPool.setWfNo(wfNo);
        noPool.setIsPretty(isPretty != null && isPretty);
        noPool.setStatus(STATUS_UNUSED);
        noPool.setUserId(null);
        noPool.setCreateTime(LocalDateTime.now());
        noPool.setUpdateTime(LocalDateTime.now());
        wfNoPoolMapper.insert(noPool);
    }

    /**
     * 生成随机号码（非顺序递增）
     * 系统自动生成固定10位数的号码
     * 使用时间戳 + 随机数 + 纳秒数的方式，确保非顺序性和唯一性
     *
     * @return 生成的10位数号码
     */
    private Long generateRandomNumber() {
        // 使用时间戳 + 随机数 + 纳秒数，确保非顺序性
        long timestamp = System.currentTimeMillis();
        long randomValue = random.nextLong() & 0x7FFFFFFF; // 正随机数
        long nanoTime = System.nanoTime();

        // 混合计算，确保非顺序性
        long mixed = (timestamp % 1000000000L) + (randomValue % 1000000000L) + (nanoTime % 1000000000L);
        
        // 映射到10位数范围 [1000000000, 9999999999]
        long number = AUTO_GEN_MIN + (mixed % (AUTO_GEN_MAX - AUTO_GEN_MIN + 1));
        
        // 确保在有效范围内
        if (number < AUTO_GEN_MIN) {
            number = AUTO_GEN_MIN + (Math.abs(random.nextLong()) % (AUTO_GEN_MAX - AUTO_GEN_MIN + 1));
        }
        if (number > AUTO_GEN_MAX) {
            number = AUTO_GEN_MIN + (number % (AUTO_GEN_MAX - AUTO_GEN_MIN + 1));
        }
        
        return number;
    }

    @Override
    public PageResult<WfNoPool> queryPage(NoPoolQueryDTO queryDTO) {
        // 构建查询条件
        LambdaQueryWrapper<WfNoPool> query = new LambdaQueryWrapper<>();
        
        if (queryDTO.getWfNo() != null) {
            query.eq(WfNoPool::getWfNo, queryDTO.getWfNo());
        }
        if (queryDTO.getStatus() != null) {
            query.eq(WfNoPool::getStatus, queryDTO.getStatus());
        }
        if (queryDTO.getIsPretty() != null) {
            query.eq(WfNoPool::getIsPretty, queryDTO.getIsPretty());
        }
        if (queryDTO.getUserId() != null) {
            query.eq(WfNoPool::getUserId, queryDTO.getUserId());
        }
        
        // 按创建时间倒序
        query.orderByDesc(WfNoPool::getCreateTime);
        
        // 分页查询
        Page<WfNoPool> page = new Page<>(queryDTO.getPage(), queryDTO.getSize());
        Page<WfNoPool> result = wfNoPoolMapper.selectPage(page, query);
        
        return new PageResult<>(
                result.getRecords(),
                result.getTotal(),
                (int) result.getCurrent(),
                (int) result.getSize()
        );
    }

    @Override
    public WfNoPool getById(Long id) {
        if (id == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        WfNoPool noPool = wfNoPoolMapper.selectById(id);
        if (noPool == null) {
            throw new BaseException(ErrorCode.NUMBER_NOT_FOUND);
        }
        return noPool;
    }

    @Override
    public WfNoPool getByWfNo(Long wfNo) {
        if (wfNo == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        LambdaQueryWrapper<WfNoPool> query = new LambdaQueryWrapper<>();
        query.eq(WfNoPool::getWfNo, wfNo);
        WfNoPool noPool = wfNoPoolMapper.selectOne(query);
        if (noPool == null) {
            throw new BaseException(ErrorCode.NUMBER_NOT_FOUND);
        }
        return noPool;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(NoPoolUpdateDTO updateDTO) {
        if (updateDTO.getId() == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        
        // 查询号码是否存在
        WfNoPool noPool = wfNoPoolMapper.selectById(updateDTO.getId());
        if (noPool == null) {
            throw new BaseException(ErrorCode.NUMBER_NOT_FOUND);
        }
        
        // 更新字段
        boolean needUpdate = false;
        if (updateDTO.getStatus() != null && !updateDTO.getStatus().equals(noPool.getStatus())) {
            noPool.setStatus(updateDTO.getStatus());
            needUpdate = true;
        }
        if (updateDTO.getIsPretty() != null && !updateDTO.getIsPretty().equals(noPool.getIsPretty())) {
            noPool.setIsPretty(updateDTO.getIsPretty());
            needUpdate = true;
        }
        if (updateDTO.getUserId() != null && !updateDTO.getUserId().equals(noPool.getUserId())) {
            noPool.setUserId(updateDTO.getUserId());
            needUpdate = true;
        } else if (updateDTO.getUserId() == null && noPool.getUserId() != null) {
            // 释放号码（userId设为null）
            noPool.setUserId(null);
            if (noPool.getStatus() == 1) {
                // 如果已使用，释放时改为未使用
                noPool.setStatus(STATUS_UNUSED);
            }
            needUpdate = true;
        }
        
        if (needUpdate) {
            noPool.setUpdateTime(LocalDateTime.now());
            wfNoPoolMapper.updateById(noPool);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteById(Long id) {
        if (id == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        
        WfNoPool noPool = wfNoPoolMapper.selectById(id);
        if (noPool == null) {
            throw new BaseException(ErrorCode.NUMBER_NOT_FOUND);
        }
        
        // 如果号码已被使用，不允许删除
        if (noPool.getStatus() == 1 && noPool.getUserId() != null) {
            throw new BaseException(ErrorCode.NUMBER_IN_USE);
        }
        
        wfNoPoolMapper.deleteById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByWfNo(Long wfNo) {
        if (wfNo == null) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        
        LambdaQueryWrapper<WfNoPool> query = new LambdaQueryWrapper<>();
        query.eq(WfNoPool::getWfNo, wfNo);
        WfNoPool noPool = wfNoPoolMapper.selectOne(query);
        
        if (noPool == null) {
            throw new BaseException(ErrorCode.NUMBER_NOT_FOUND);
        }
        
        // 如果号码已被使用，不允许删除
        if (noPool.getStatus() == 1 && noPool.getUserId() != null) {
            throw new BaseException(ErrorCode.NUMBER_IN_USE);
        }
        
        wfNoPoolMapper.delete(query);
    }
}

