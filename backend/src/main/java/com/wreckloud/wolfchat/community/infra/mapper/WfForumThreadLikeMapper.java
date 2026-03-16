package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumThreadLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Description 主题点赞关系 Mapper
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Mapper
public interface WfForumThreadLikeMapper extends BaseMapper<WfForumThreadLike> {
    @Select("<script>" +
            "SELECT thread_id " +
            "FROM wf_forum_thread_like " +
            "WHERE user_id = #{userId} " +
            "AND thread_id IN " +
            "<foreach collection='threadIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectLikedThreadIds(@Param("userId") Long userId, @Param("threadIds") List<Long> threadIds);
}
