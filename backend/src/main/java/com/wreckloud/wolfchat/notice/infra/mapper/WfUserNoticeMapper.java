package com.wreckloud.wolfchat.notice.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.notice.domain.entity.WfUserNotice;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description 用户通知 Mapper
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Mapper
public interface WfUserNoticeMapper extends BaseMapper<WfUserNotice> {
    @Select("SELECT COUNT(1) FROM wf_user_notice WHERE user_id = #{userId} AND is_read = 0")
    Long selectUnreadCountByUserId(@Param("userId") Long userId);
}
