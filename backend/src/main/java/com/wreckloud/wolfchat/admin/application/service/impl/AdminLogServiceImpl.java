package com.wreckloud.wolfchat.admin.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.admin.api.vo.AdminLogVO;
import com.wreckloud.wolfchat.admin.application.service.AdminLogService;
import com.wreckloud.wolfchat.admin.domain.entity.WfAdminLog;
import com.wreckloud.wolfchat.admin.infra.mapper.WfAdminLogMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 管理员-操作日志服务实现类
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@Service
public class AdminLogServiceImpl implements AdminLogService {
    
    @Autowired
    private WfAdminLogMapper adminLogMapper;
    
    @Override
    public void saveLog(WfAdminLog adminLog) {
        adminLogMapper.insert(adminLog);
        log.info("管理员操作日志已记录：adminId={}, action={}, targetType={}, targetId={}", 
                adminLog.getAdminId(), adminLog.getAction(), adminLog.getTargetType(), adminLog.getTargetId());
    }
    
    @Override
    public Page<AdminLogVO> getLogList(Long current, Long size, Long adminId, String action) {
        log.info("查询管理员操作日志：current={}, size={}, adminId={}, action={}", current, size, adminId, action);
        
        // 构建查询条件
        LambdaQueryWrapper<WfAdminLog> wrapper = new LambdaQueryWrapper<>();
        
        // 管理员ID筛选
        if (adminId != null) {
            wrapper.eq(WfAdminLog::getAdminId, adminId);
        }
        
        // 操作类型筛选
        if (StringUtils.isNotBlank(action)) {
            wrapper.eq(WfAdminLog::getAction, action);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(WfAdminLog::getCreateTime);
        
        // 分页查询
        Page<WfAdminLog> page = adminLogMapper.selectPage(new Page<>(current, size), wrapper);
        
        // 转换为VO
        Page<AdminLogVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AdminLogVO> voList = page.getRecords().stream()
                .map(this::convertToAdminLogVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        
        return result;
    }
    
    /**
     * 转换为AdminLogVO
     */
    private AdminLogVO convertToAdminLogVO(WfAdminLog adminLog) {
        AdminLogVO vo = new AdminLogVO();
        BeanUtils.copyProperties(adminLog, vo);
        return vo;
    }
}

