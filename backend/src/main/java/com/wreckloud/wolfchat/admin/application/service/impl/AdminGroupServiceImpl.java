package com.wreckloud.wolfchat.admin.application.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.admin.api.dto.GroupDismissDTO;
import com.wreckloud.wolfchat.admin.api.vo.AdminGroupVO;
import com.wreckloud.wolfchat.admin.application.service.AdminGroupService;
import com.wreckloud.wolfchat.admin.application.service.AdminLogService;
import com.wreckloud.wolfchat.admin.domain.entity.WfAdminLog;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.group.domain.entity.WfGroup;
import com.wreckloud.wolfchat.group.domain.enums.GroupStatus;
import com.wreckloud.wolfchat.group.infra.mapper.WfGroupMapper;
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
 * @Description 管理员-群组管理服务实现类
 * @Author Wreckloud
 * @Date 2024-12-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminGroupServiceImpl implements AdminGroupService {
    
    private final WfGroupMapper groupMapper;
    private final WfUserMapper userMapper;
    private final AdminLogService adminLogService;
    
    @Override
    public Page<AdminGroupVO> getGroupList(Long current, Long size, String keyword, Integer status) {
        log.info("管理员查询群组列表：current={}, size={}, keyword={}, status={}", current, size, keyword, status);
        
        // 构建查询条件
        LambdaQueryWrapper<WfGroup> wrapper = new LambdaQueryWrapper<>();
        
        // 关键词搜索（群名称）
        if (StringUtils.isNotBlank(keyword)) {
            wrapper.like(WfGroup::getGroupName, keyword);
        }
        
        // 状态筛选
        if (status != null) {
            wrapper.eq(WfGroup::getStatus, status);
        }
        
        // 按创建时间倒序
        wrapper.orderByDesc(WfGroup::getCreateTime);
        
        // 分页查询
        Page<WfGroup> page = groupMapper.selectPage(new Page<>(current, size), wrapper);
        
        // 转换为VO
        Page<AdminGroupVO> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<AdminGroupVO> voList = page.getRecords().stream()
                .map(this::convertToAdminGroupVO)
                .collect(Collectors.toList());
        result.setRecords(voList);
        
        return result;
    }
    
    @Override
    public AdminGroupVO getGroupDetail(Long groupId) {
        log.info("管理员查询群组详情：groupId={}", groupId);
        
        WfGroup group = groupMapper.selectById(groupId);
        if (group == null) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }
        
        return convertToAdminGroupVO(group);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void dismissGroup(GroupDismissDTO dto, Long adminId) {
        log.info("管理员解散群组：dto={}, adminId={}", dto, adminId);
        
        // 1. 查询群组
        WfGroup group = groupMapper.selectById(dto.getGroupId());
        if (group == null) {
            throw new BaseException(ErrorCode.GROUP_NOT_FOUND);
        }
        
        if (group.getStatus().equals(GroupStatus.DISBANDED.getCode())) {
            throw new BaseException(ErrorCode.GROUP_DISBANDED);
        }
        
        // 2. 更新群组状态为已解散
        LambdaUpdateWrapper<WfGroup> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WfGroup::getId, dto.getGroupId())
                .set(WfGroup::getStatus, GroupStatus.DISBANDED.getCode());
        int rows = groupMapper.update(null, wrapper);
        
        if (rows == 0) {
            throw new BaseException(ErrorCode.INVALID_PARAM);
        }
        
        // 3. 记录操作日志
        WfUser admin = userMapper.selectById(adminId);
        
        WfAdminLog adminLog = WfAdminLog.builder()
                .adminId(adminId)
                .adminName(admin.getUsername())
                .adminWfNo(admin.getWfNo())
                .action("dismiss_group")
                .targetType("group")
                .targetId(group.getId())
                .targetName(group.getGroupName())
                .details(String.format("群主ID：%d，成员数：%d，原因：%s", 
                        group.getOwnerId(), group.getMemberCount(),
                        StringUtils.defaultIfBlank(dto.getReason(), "无")))
                .ipAddress(getClientIp())
                .userAgent(getUserAgent())
                .result(1)
                .build();
        
        adminLogService.saveLog(adminLog);
        
        log.info("群组解散成功：groupId={}, groupName={}", group.getId(), group.getGroupName());
    }
    
    /**
     * 转换为AdminGroupVO
     */
    private AdminGroupVO convertToAdminGroupVO(WfGroup group) {
        AdminGroupVO vo = new AdminGroupVO();
        BeanUtils.copyProperties(group, vo);
        
        // 查询群主信息
        WfUser owner = userMapper.selectById(group.getOwnerId());
        if (owner != null) {
            vo.setOwnerName(owner.getUsername());
            vo.setOwnerWfNo(owner.getWfNo());
        }
        
        return vo;
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

