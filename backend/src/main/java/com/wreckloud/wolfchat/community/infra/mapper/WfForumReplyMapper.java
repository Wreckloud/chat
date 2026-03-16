package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReply;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description 论坛回复 Mapper
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Mapper
public interface WfForumReplyMapper extends BaseMapper<WfForumReply> {
    /**
     * 查询指定主题当前最大楼层
     */
    @Select("SELECT COALESCE(MAX(floor_no), 1) FROM wf_forum_reply WHERE thread_id = #{threadId}")
    Integer selectMaxFloorNo(@Param("threadId") Long threadId);

    @Select("SELECT COALESCE(SUM(r.like_count), 0) " +
            "FROM wf_forum_reply r " +
            "INNER JOIN wf_forum_thread t ON t.id = r.thread_id " +
            "WHERE r.author_id = #{authorId} " +
            "AND r.status = 'NORMAL' " +
            "AND t.status <> 'DELETED'")
    Long selectLikeCountSumByAuthorId(@Param("authorId") Long authorId);

    @Select("SELECT COUNT(1) " +
            "FROM wf_forum_reply r " +
            "INNER JOIN wf_forum_thread t ON t.id = r.thread_id " +
            "WHERE r.author_id = #{authorId} " +
            "AND r.status = 'NORMAL' " +
            "AND t.status <> 'DELETED'")
    Long selectVisibleCountByAuthorId(@Param("authorId") Long authorId);
}
