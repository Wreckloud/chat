package com.wreckloud.wolfchat.follow.infra.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wreckloud.wolfchat.follow.domain.entity.WfFollow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * @Description 关注关系 Mapper
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Mapper
public interface WfFollowMapper extends BaseMapper<WfFollow> {
    @Select("SELECT COUNT(1) " +
            "FROM wf_follow f1 " +
            "INNER JOIN wf_follow f2 " +
            "ON f1.follower_id = f2.followee_id AND f1.followee_id = f2.follower_id " +
            "WHERE f1.follower_id = #{userId} " +
            "AND f1.status = 'FOLLOWING' " +
            "AND f2.status = 'FOLLOWING'")
    Long selectMutualCountByUserId(@Param("userId") Long userId);
}
