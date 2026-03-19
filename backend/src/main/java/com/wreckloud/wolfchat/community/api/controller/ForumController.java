package com.wreckloud.wolfchat.community.api.controller;

import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.community.api.dto.CreateReplyDTO;
import com.wreckloud.wolfchat.community.api.dto.CreateThreadDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateLikeDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadEssenceDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadLockDTO;
import com.wreckloud.wolfchat.community.api.dto.UpdateThreadStickyDTO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumReplyVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadDetailVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadPageVO;
import com.wreckloud.wolfchat.community.api.vo.ForumThreadVO;
import com.wreckloud.wolfchat.community.application.service.ForumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Description 论坛控制器
 * @Author Wreckloud
 * @Date 2026-03-10
 */
@Tag(name = "论坛", description = "主题与回复相关接口")
@RestController
@RequestMapping("/forum")
@RequiredArgsConstructor
public class ForumController {
    private final ForumService forumService;

    @Operation(summary = "社区信息流", description = "按推荐/热议/好友/最新获取帖子流")
    @GetMapping("/feed")
    public Result<ForumThreadPageVO> listFeedThreads(@RequestParam(defaultValue = "recommend") String tab,
                                                      @RequestParam(defaultValue = "1") long page,
                                                      @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(forumService.listFeedThreads(userId, page, size, tab));
    }

    @Operation(summary = "发布主题", description = "发布主题到社区")
    @PostMapping("/threads")
    public Result<ForumThreadVO> createThread(@RequestBody @Validated CreateThreadDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        ForumThreadVO thread = forumService.createThread(userId, dto);
        return Result.success("发布成功", thread);
    }

    @Operation(summary = "主题详情", description = "获取主题详情")
    @GetMapping("/threads/{threadId}")
    public Result<ForumThreadDetailVO> getThreadDetail(@PathVariable Long threadId) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(forumService.getThreadDetail(userId, threadId));
    }

    @Operation(summary = "主题浏览上报", description = "增加主题浏览数")
    @PutMapping("/threads/{threadId}/view")
    public Result<Void> increaseThreadView(@PathVariable Long threadId) {
        forumService.increaseThreadView(threadId);
        return Result.success("操作成功", null);
    }

    @Operation(summary = "点赞/取消点赞主题", description = "更新当前用户对主题的点赞状态")
    @PutMapping("/threads/{threadId}/like")
    public Result<Void> updateThreadLikeStatus(@PathVariable Long threadId,
                                               @RequestBody @Validated UpdateLikeDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        forumService.updateThreadLikeStatus(userId, threadId, Boolean.TRUE.equals(dto.getLiked()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "锁定/解锁主题", description = "更新主题锁定状态（仅主题作者）")
    @PutMapping("/threads/{threadId}/lock")
    public Result<Void> updateThreadLockStatus(@PathVariable Long threadId,
                                               @RequestBody @Validated UpdateThreadLockDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        forumService.updateThreadLockStatus(userId, threadId, Boolean.TRUE.equals(dto.getLocked()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "置顶/取消置顶主题", description = "更新主题置顶状态（仅主题作者）")
    @PutMapping("/threads/{threadId}/sticky")
    public Result<Void> updateThreadStickyStatus(@PathVariable Long threadId,
                                                 @RequestBody @Validated UpdateThreadStickyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        forumService.updateThreadStickyStatus(userId, threadId, Boolean.TRUE.equals(dto.getSticky()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "设置/取消精华主题", description = "更新主题精华状态（仅主题作者）")
    @PutMapping("/threads/{threadId}/essence")
    public Result<Void> updateThreadEssenceStatus(@PathVariable Long threadId,
                                                  @RequestBody @Validated UpdateThreadEssenceDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        forumService.updateThreadEssenceStatus(userId, threadId, Boolean.TRUE.equals(dto.getEssence()));
        return Result.success("操作成功", null);
    }

    @Operation(summary = "删除主题", description = "逻辑删除主题（仅主题作者）")
    @DeleteMapping("/threads/{threadId}")
    public Result<Void> deleteThread(@PathVariable Long threadId) {
        Long userId = UserContext.getRequiredUserId();
        forumService.deleteThread(userId, threadId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "回复列表", description = "分页获取主题回复列表")
    @GetMapping("/threads/{threadId}/replies")
    public Result<ForumReplyPageVO> listThreadReplies(@PathVariable Long threadId,
                                                       @RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long size) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(forumService.listThreadReplies(userId, threadId, page, size));
    }

    @Operation(summary = "发布回复", description = "对指定主题发布回复")
    @PostMapping("/threads/{threadId}/replies")
    public Result<ForumReplyVO> createReply(@PathVariable Long threadId,
                                            @RequestBody @Validated CreateReplyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        ForumReplyVO reply = forumService.createReply(userId, threadId, dto);
        return Result.success("回复成功", reply);
    }

    @Operation(summary = "删除回复", description = "逻辑删除回复（回复作者或主题作者）")
    @DeleteMapping("/replies/{replyId}")
    public Result<Void> deleteReply(@PathVariable Long replyId) {
        Long userId = UserContext.getRequiredUserId();
        forumService.deleteReply(userId, replyId);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "点赞/取消点赞回复", description = "更新当前用户对回复的点赞状态")
    @PutMapping("/replies/{replyId}/like")
    public Result<Void> updateReplyLikeStatus(@PathVariable Long replyId,
                                              @RequestBody @Validated UpdateLikeDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        forumService.updateReplyLikeStatus(userId, replyId, Boolean.TRUE.equals(dto.getLiked()));
        return Result.success("操作成功", null);
    }
}
