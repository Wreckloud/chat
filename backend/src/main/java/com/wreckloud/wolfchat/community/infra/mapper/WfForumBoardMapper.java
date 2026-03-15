package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumBoard;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 论坛版块 Mapper
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Mapper
public interface WfForumBoardMapper extends BaseMapper<WfForumBoard> {
}
