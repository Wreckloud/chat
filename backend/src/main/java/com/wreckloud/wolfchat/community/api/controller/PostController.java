package com.wreckloud.wolfchat.community.api.controller;

import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.community.api.dto.CreateCommentDTO;
import com.wreckloud.wolfchat.community.api.dto.CreatePostDTO;
import com.wreckloud.wolfchat.community.api.vo.PostDetailVO;
import com.wreckloud.wolfchat.community.api.vo.PostPageVO;
import com.wreckloud.wolfchat.community.api.vo.PostVO;
import com.wreckloud.wolfchat.community.application.service.PostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * @Description 社区帖子控制器
 * @Author Wreckloud
 * @Date 2026-01-23
 */
@Tag(name = "社区", description = "帖子与评论相关接口")
@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {
    private final PostService postService;

    @Operation(summary = "发布帖子", description = "发布一条帖子")
    @PostMapping
    public Result<PostVO> createPost(@RequestBody @Validated CreatePostDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        PostVO post = postService.createPost(userId, dto);
        return Result.success("发布成功", post);
    }

    @Operation(summary = "帖子列表", description = "分页获取帖子列表")
    @GetMapping
    public Result<PostPageVO> listPosts(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size) {
        PostPageVO pageVO = postService.listPosts(page, size);
        return Result.success(pageVO);
    }

    @Operation(summary = "帖子详情", description = "获取帖子详情与评论列表")
    @GetMapping("/{postId}")
    public Result<PostDetailVO> getPostDetail(@PathVariable Long postId) {
        PostDetailVO detailVO = postService.getPostDetail(postId);
        return Result.success(detailVO);
    }

    @Operation(summary = "发布评论", description = "对指定帖子发布评论")
    @PostMapping("/{postId}/comments")
    public Result<Void> addComment(@PathVariable Long postId,
                                   @RequestBody @Validated CreateCommentDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        postService.addComment(userId, postId, dto);
        return Result.success("评论成功", null);
    }

}
