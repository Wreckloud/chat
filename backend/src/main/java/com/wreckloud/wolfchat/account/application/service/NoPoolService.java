package com.wreckloud.wolfchat.account.application.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.api.dto.NoPoolQueryDTO;
import com.wreckloud.wolfchat.account.api.dto.NoPoolUpdateDTO;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.common.web.PageResult;

/**
 * @Description 号码池服务接口
 * @Author Wreckloud
 * @Date 2025-12-07
 */
public interface NoPoolService {

    /**
     * 自动生成普通号码
     * 从1000000开始，非顺序递增
     *
     * @param count 生成数量
     * @return 成功生成的数量
     */
    int generateNumbers(int count);

    /**
     * 管理员手动添加号码
     *
     * @param wfNo     号码
     * @param isPretty 是否靓号
     */
    void addNumber(Long wfNo, Boolean isPretty);

    /**
     * 分页查询号码池
     *
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    PageResult<WfNoPool> queryPage(NoPoolQueryDTO queryDTO);

    /**
     * 根据ID查询号码
     *
     * @param id 号码ID
     * @return 号码信息
     */
    WfNoPool getById(Long id);

    /**
     * 根据号码查询
     *
     * @param wfNo 号码
     * @return 号码信息
     */
    WfNoPool getByWfNo(Long wfNo);

    /**
     * 更新号码信息
     *
     * @param updateDTO 更新请求
     */
    void update(NoPoolUpdateDTO updateDTO);

    /**
     * 根据ID删除号码
     *
     * @param id 号码ID
     */
    void deleteById(Long id);

    /**
     * 根据号码删除
     *
     * @param wfNo 号码
     */
    void deleteByWfNo(Long wfNo);
}

