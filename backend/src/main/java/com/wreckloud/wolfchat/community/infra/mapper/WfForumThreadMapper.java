package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThread;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * @Description 论坛主题 Mapper
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Mapper
public interface WfForumThreadMapper extends BaseMapper<WfForumThread> {
    @Select("SELECT COUNT(1) AS threadCount, " +
            "IFNULL(SUM(CASE WHEN reply_count > 0 THEN reply_count ELSE 0 END), 0) AS replyCount " +
            "FROM wf_forum_thread " +
            "WHERE board_id = #{boardId} AND status <> 'DELETED'")
    Map<String, Object> selectBoardStats(@Param("boardId") Long boardId);

    @Select("SELECT * FROM wf_forum_thread " +
            "WHERE board_id = #{boardId} AND status <> 'DELETED' " +
            "ORDER BY last_reply_time DESC, create_time DESC, id DESC LIMIT 1")
    WfForumThread selectLatestVisibleByBoardId(@Param("boardId") Long boardId);

    @Select("SELECT COALESCE(SUM(view_count), 0) " +
            "FROM wf_forum_thread " +
            "WHERE author_id = #{authorId} AND status <> 'DELETED'")
    Long selectViewCountSumByAuthorId(@Param("authorId") Long authorId);

    @Select("SELECT COALESCE(SUM(like_count), 0) " +
            "FROM wf_forum_thread " +
            "WHERE author_id = #{authorId} AND status <> 'DELETED'")
    Long selectLikeCountSumByAuthorId(@Param("authorId") Long authorId);
}
