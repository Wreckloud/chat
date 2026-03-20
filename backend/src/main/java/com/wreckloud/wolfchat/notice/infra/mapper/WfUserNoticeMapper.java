package com.wreckloud.wolfchat.notice.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.notice.api.vo.UserNoticeUnreadSummaryVO;
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

    @Select("SELECT " +
            "COUNT(1) AS totalUnread, " +
            "COALESCE(SUM(CASE WHEN notice_type = 'ACHIEVEMENT_UNLOCK' THEN 1 ELSE 0 END), 0) AS achievementUnread, " +
            "COALESCE(SUM(CASE WHEN notice_type = 'FOLLOW_RECEIVED' THEN 1 ELSE 0 END), 0) AS followUnread, " +
            "COALESCE(SUM(CASE WHEN notice_type IN ('THREAD_LIKED', 'THREAD_REPLIED', 'REPLY_LIKED', 'REPLY_REPLIED', 'CHAT_MESSAGE_REPLIED', 'LOBBY_MESSAGE_REPLIED') THEN 1 ELSE 0 END), 0) AS interactionUnread " +
            "FROM wf_user_notice " +
            "WHERE user_id = #{userId} AND is_read = 0")
    UserNoticeUnreadSummaryVO selectUnreadSummaryByUserId(@Param("userId") Long userId);
}
