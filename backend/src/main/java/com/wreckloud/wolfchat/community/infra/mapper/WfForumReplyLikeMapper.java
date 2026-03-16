package com.wreckloud.wolfchat.community.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.community.domain.entity.WfForumReplyLike;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * @Description 回复点赞关系 Mapper
 * @Author Wreckloud
 * @Date 2026-03-16
 */
@Mapper
public interface WfForumReplyLikeMapper extends BaseMapper<WfForumReplyLike> {
    @Select("<script>" +
            "SELECT reply_id " +
            "FROM wf_forum_reply_like " +
            "WHERE user_id = #{userId} " +
            "AND reply_id IN " +
            "<foreach collection='replyIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Long> selectLikedReplyIds(@Param("userId") Long userId, @Param("replyIds") List<Long> replyIds);
}
