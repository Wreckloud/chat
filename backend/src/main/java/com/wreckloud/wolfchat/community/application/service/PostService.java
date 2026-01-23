package com.wreckloud.wolfchat.community.application.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.community.api.dto.CreateCommentDTO;
import com.wreckloud.wolfchat.community.api.dto.CreatePostDTO;
import com.wreckloud.wolfchat.community.api.vo.CommentVO;
import com.wreckloud.wolfchat.community.api.vo.PostDetailVO;
import com.wreckloud.wolfchat.common.enums.CommentStatus;
import com.wreckloud.wolfchat.common.enums.PostStatus;
import com.wreckloud.wolfchat.community.api.vo.PostPageVO;
import com.wreckloud.wolfchat.community.api.vo.PostVO;
import com.wreckloud.wolfchat.community.api.vo.UserBriefVO;
import com.wreckloud.wolfchat.community.domain.entity.WfComment;
import com.wreckloud.wolfchat.community.domain.entity.WfPost;
import com.wreckloud.wolfchat.community.infra.mapper.WfCommentMapper;
import com.wreckloud.wolfchat.community.infra.mapper.WfPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @Description 帖子与评论服务
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Service
@RequiredArgsConstructor
public class PostService {
    private final WfPostMapper wfPostMapper;
    private final WfCommentMapper wfCommentMapper;
    private final WfUserMapper wfUserMapper;

    /**
     * 发布帖子
     */
    @Transactional(rollbackFor = Exception.class)
    public PostVO createPost(Long userId, CreatePostDTO dto) {
        WfPost post = new WfPost();
        post.setUserId(userId);
        post.setContent(dto.getContent());
        post.setRoomId(dto.getRoomId());
        post.setStatus(PostStatus.NORMAL);
        wfPostMapper.insert(post);

        WfUser user = wfUserMapper.selectById(userId);
        return toPostVO(post, user);
    }

    /**
     * 获取帖子列表
     */
    public PostPageVO listPosts(long page, long size) {
        Page<WfPost> pageReq = new Page<>(page, size);
        LambdaQueryWrapper<WfPost> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfPost::getStatus, PostStatus.NORMAL)
                .orderByDesc(WfPost::getCreateTime);
        Page<WfPost> result = wfPostMapper.selectPage(pageReq, queryWrapper);

        List<WfPost> records = result.getRecords();
        Map<Long, WfUser> userMap = getUserMap(records.stream()
                .map(WfPost::getUserId)
                .collect(Collectors.toList()));

        List<PostVO> list = new ArrayList<>();
        for (WfPost post : records) {
            WfUser user = userMap.get(post.getUserId());
            list.add(toPostVO(post, user));
        }

        PostPageVO pageVO = new PostPageVO();
        pageVO.setList(list);
        pageVO.setTotal(result.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        return pageVO;
    }

    /**
     * 获取帖子详情
     */
    public PostDetailVO getPostDetail(Long postId) {
        WfPost post = wfPostMapper.selectById(postId);
        if (post == null || PostStatus.DELETED.equals(post.getStatus())) {
            throw new BaseException(ErrorCode.POST_NOT_FOUND);
        }

        WfUser user = wfUserMapper.selectById(post.getUserId());
        PostVO postVO = toPostVO(post, user);

        List<CommentVO> comments = getComments(postId);
        PostDetailVO detailVO = new PostDetailVO();
        detailVO.setPost(postVO);
        detailVO.setComments(comments);
        return detailVO;
    }

    /**
     * 发布评论
     */
    @Transactional(rollbackFor = Exception.class)
    public void addComment(Long userId, Long postId, CreateCommentDTO dto) {
        WfPost post = wfPostMapper.selectById(postId);
        if (post == null || PostStatus.DELETED.equals(post.getStatus())) {
            throw new BaseException(ErrorCode.POST_NOT_FOUND);
        }

        WfComment comment = new WfComment();
        comment.setPostId(postId);
        comment.setUserId(userId);
        comment.setContent(dto.getContent());
        comment.setStatus(CommentStatus.NORMAL);
        wfCommentMapper.insert(comment);
    }

    /**
     * 获取评论列表
     */
    public List<CommentVO> getComments(Long postId) {
        LambdaQueryWrapper<WfComment> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(WfComment::getPostId, postId)
                .eq(WfComment::getStatus, CommentStatus.NORMAL)
                .orderByAsc(WfComment::getCreateTime);
        List<WfComment> comments = wfCommentMapper.selectList(queryWrapper);

        Map<Long, WfUser> userMap = getUserMap(comments.stream()
                .map(WfComment::getUserId)
                .collect(Collectors.toList()));

        List<CommentVO> list = new ArrayList<>();
        for (WfComment comment : comments) {
            WfUser user = userMap.get(comment.getUserId());
            list.add(toCommentVO(comment, user));
        }
        return list;
    }

    private Map<Long, WfUser> getUserMap(List<Long> userIds) {
        if (userIds.isEmpty()) {
            return new HashMap<>();
        }
        List<WfUser> users = wfUserMapper.selectBatchIds(userIds);
        Map<Long, WfUser> map = new HashMap<>();
        for (WfUser user : users) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private PostVO toPostVO(WfPost post, WfUser user) {
        PostVO vo = new PostVO();
        vo.setPostId(post.getId());
        vo.setContent(post.getContent());
        vo.setRoomId(post.getRoomId());
        vo.setCreateTime(post.getCreateTime());
        vo.setAuthor(toUserBriefVO(user));
        return vo;
    }

    private CommentVO toCommentVO(WfComment comment, WfUser user) {
        CommentVO vo = new CommentVO();
        vo.setCommentId(comment.getId());
        vo.setContent(comment.getContent());
        vo.setCreateTime(comment.getCreateTime());
        vo.setAuthor(toUserBriefVO(user));
        return vo;
    }

    private UserBriefVO toUserBriefVO(WfUser user) {
        if (user == null) {
            return null;
        }
        UserBriefVO vo = new UserBriefVO();
        vo.setUserId(user.getId());
        vo.setWolfNo(user.getWolfNo());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        return vo;
    }
}
