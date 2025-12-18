package com.wreckloud.wolfchat.group.api.controller;

import com.wreckloud.wolfchat.common.security.annotation.RequireLogin;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.group.api.dto.GroupMuteDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupNoticePublishDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupTransferDTO;
import com.wreckloud.wolfchat.group.api.vo.GroupNoticeVO;
import com.wreckloud.wolfchat.group.application.service.GroupSettingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Description 群设置控制器
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Tag(name = "群设置管理", description = "群设置相关接口，包括群公告、禁言、转让群主等")
@RestController
@RequestMapping("/group/setting")
@Validated
public class GroupSettingController {

    @Autowired
    private GroupSettingService groupSettingService;

    @Operation(
            summary = "发布群公告",
            description = "发布群组公告。仅群主和管理员有权限发布。如果设置为置顶，会取消其他公告的置顶状态。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发布成功", content = @Content(schema = @Schema(implementation = GroupNoticeVO.class))),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主/管理员可发布）"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @PostMapping("/{groupId}/notice")
    public Result<GroupNoticeVO> publishNotice(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "发布群公告请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupNoticePublishDTO.class))
            )
            @RequestBody @Validated GroupNoticePublishDTO dto,
            @Parameter(description = "发布者用户ID（实际应从Token中获取）", example = "1")
            @RequestHeader(value = "User-Id", required = false, defaultValue = "1") Long userId) {
        GroupNoticeVO noticeVO = groupSettingService.publishNotice(
                groupId, dto.getTitle(), dto.getContent(), dto.getIsPinned(), userId);
        return Result.ok(noticeVO);
    }

    @Operation(
            summary = "查询群公告列表",
            description = "查询群组的所有公告。按置顶和发布时间排序。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @GetMapping("/{groupId}/notices")
    public Result<List<GroupNoticeVO>> getNotices(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        List<GroupNoticeVO> notices = groupSettingService.getNotices(groupId, userId);
        return Result.ok(notices);
    }

    @Operation(
            summary = "删除群公告",
            description = "删除群组公告。仅群主和管理员有权限删除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主/管理员可删除）")
    })
    @RequireLogin
    @DeleteMapping("/{groupId}/notice/{noticeId}")
    public Result<Void> deleteNotice(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @Parameter(description = "公告ID", required = true, example = "1")
            @PathVariable Long noticeId) {
        Long userId = UserContext.getUserId();
        groupSettingService.deleteNotice(groupId, noticeId, userId);
        return Result.ok();
    }

    @Operation(
            summary = "全员禁言/解除全员禁言",
            description = "设置或取消全员禁言。仅群主有权限操作。全员禁言后，只有群主和管理员可以发言。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主可设置）")
    })
    @RequireLogin
    @PutMapping("/{groupId}/mute-all")
    public Result<Void> muteAll(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @Parameter(description = "是否全员禁言", required = true, example = "true")
            @RequestParam Boolean isMuted) {
        Long userId = UserContext.getUserId();
        groupSettingService.muteAll(groupId, isMuted, userId);
        return Result.ok();
    }

    @Operation(
            summary = "禁言/解禁单个成员",
            description = "禁言或解禁单个群成员。群主和管理员有权限操作，但不能禁言群主，管理员不能禁言其他管理员。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PutMapping("/{groupId}/member/{targetUserId}/mute")
    public Result<Void> muteMember(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @Parameter(description = "被禁言用户ID", required = true, example = "2")
            @PathVariable Long targetUserId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "禁言设置请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupMuteDTO.class))
            )
            @RequestBody @Validated GroupMuteDTO dto) {
        Long userId = UserContext.getUserId();
        groupSettingService.muteMember(groupId, targetUserId, dto.getIsMuted(), dto.getMuteDuration(), userId);
        return Result.ok();
    }

    @Operation(
            summary = "转让群主",
            description = "将群主权限转让给其他成员。仅当前群主有权限操作。转让后，原群主变为普通成员。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "转让成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主可转让）"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PostMapping("/{groupId}/transfer")
    public Result<Void> transferOwner(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "转让群主请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupTransferDTO.class))
            )
            @RequestBody @Validated GroupTransferDTO dto) {
        Long userId = UserContext.getUserId();
        groupSettingService.transferOwner(groupId, dto.getNewOwnerId(), userId);
        return Result.ok();
    }
}

