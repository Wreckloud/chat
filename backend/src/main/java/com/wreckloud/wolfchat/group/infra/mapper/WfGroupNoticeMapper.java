package com.wreckloud.wolfchat.group.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.group.domain.entity.WfGroupNotice;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Description 群公告Mapper接口
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Mapper
public interface WfGroupNoticeMapper extends BaseMapper<WfGroupNotice> {
}

