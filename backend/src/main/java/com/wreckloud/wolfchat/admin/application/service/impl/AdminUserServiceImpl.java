package com.wreckloud.wolfchat.admin.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.admin.api.dto.UserStatusUpdateDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminUserVO;
import com.wreckloud.wolfchat.admin.application.service.AdminLogService;
import com.wreckloud.wolfchat.admin.application.service.AdminUserService;
import com.wreckloud.wolfchat.admin.domain.entity.WfAdminLog;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Description 管理员-用户管理服务实现类
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@Service
public class AdminUserServiceImpl implements AdminUserService {
    
    @Autowired
    private WfUserMapper userMapper;
    
    @Autowired
    private AdminLogService adminLogService;
    
    @Override
    public Page<AdminUserVO> getUserList(Long current, Long size, String keyword, Integer status) {
        log.info("管理员查询用户列表：current={}, size={}, keyword={}, status={}", current, size, keyword, status);
        
        // 构建查询条件
        LambdaQueryWrapper<WfUser> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（用户名/手机号/WF号）
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.and(w -> w
                    .like(WfUser::getUsername, keyword)
                    .or().like(WfUser::getMobile, keyword)
                    .or().eq(WfUser::getWfNo, tryParseLong(keyword))
            );
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(WfUser::getStatus, status);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(WfUser::getCreateTime);
        
        // 分页查询
        Page<WfUser> page = userMapper.selectPage(new Page<>(current, size), wrapper);
        
        // 转换为VO
        Page<AdminUserVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AdminUserVO> voList = page.getRecords().stream()
                .map(this::convertToAdminUserVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        
        return result;
    }
    
    @Override
    public AdminUserVO getUserDetail(Long userId) {
        log.info("管理员查询用户详情：userId={}", userId);
        
        WfUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        
        return convertToAdminUserVO(user);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(UserStatusUpdateDTO dto, Long adminId) {
        log.info("管理员更新用户状态：dto={}, adminId={}", dto, adminId);
        
        // 1. 查询用户
        WfUser user = userMapper.selectById(dto.getUserId());
        if (user == null) {
            throw new BaseException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 2. 更新状态
        LambdaUpdateWrapper<WfUser> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfUser::getId, dto.getUserId())
                .set(WfUser::getStatus, dto.getStatus());
        int rows = userMapper.update(null, wrapper);
        
        if (rows == 0) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        
        // 3. 记录操作日志
        WfUser admin = userMapper.selectById(adminId);
        String action = getActionByStatus(dto.getStatus());
        
        WfAdminLog adminLog = WfAdminLog.builder()
                .adminId(adminId)
                .adminName(admin.getUsername())
                .adminWfNo(admin.getWfNo())
                .action(action)
                .targetType("user")
                .targetId(user.getId())
                .targetName(user.getUsername())
                .details(String.format("原状态：%d，新状态：%d，原因：%s", 
                        user.getStatus(), dto.getStatus(), 
                        StringUtils.defaultIfBlank(dto.getReason(), "无")))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .result(1)
                .build();
        
        adminLogService.saveLog(adminLog);
        
        log.info("用户状态更新成功：userId={}, oldStatus={}, newStatus={}", 
                user.getId(), user.getStatus(), dto.getStatus());
    }
    
    /**
     * 转换为AdminUserVO
     */
    private AdminUserVO convertToAdminUserVO(WfUser user) {
        AdminUserVO vo = new AdminUserVO();
        BeanUtils.copyProperties(user, vo);
        return vo;
    }
    
    /**
     * 根据状态获取操作类型
     */
    private String getActionByStatus(Integer status) {
        return switch (status) {
            case 1 -> "enable_user";
            case 2 -> "disable_user";
            case 3 -> "delete_user";
            default -> "update_user_status";
        };
    }
    
    /**
     * 尝试将字符串解析为Long，失败返回null
     */
    private Long tryParseLong(String str) {
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * 获取客户端IP地址
     */
    private String getClientIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            return ip;
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    /**
     * 获取User-Agent
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return "unknown";
            }
            HttpServletRequest request = attributes.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            return "unknown";
        }
    }
}

