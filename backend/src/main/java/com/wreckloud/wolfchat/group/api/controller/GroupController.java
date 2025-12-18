package com.wreckloud.wolfchat.group.api.controller;

import com.wreckloud.wolfchat.common.security.annotation.RequireLogin;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.group.api.dto.GroupCreateDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupUpdateDTO;
import com.wreckloud.wolfchat.group.api.vo.GroupDetailVO;
import com.wreckloud.wolfchat.group.api.vo.GroupVO;
import com.wreckloud.wolfchat.group.application.service.GroupService;
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
 * @Description 群组管理控制器
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Tag(name = "群组管理", description = "群组相关接口，包括创建群组、查询群详情、修改群信息等")
@RestController
@RequestMapping("/group")
@Validated
public class GroupController {

    @Autowired
    private GroupService groupService;

    @Operation(
            summary = "创建群组",
            description = "创建新群组，创建者自动成为群主。需要提供群名称、初始成员列表等信息。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "创建成功", content = @Content(schema = @Schema(implementation = GroupVO.class))),
            @ApiResponse(responseCode = "-2002", description = "群成员已满"),
            @ApiResponse(responseCode = "-2010", description = "用户不存在"),
            @ApiResponse(responseCode = "-1000", description = "参数校验失败")
    })
    @RequireLogin
    @PostMapping("/create")
    public Result<GroupVO> createGroup(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "创建群组请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupCreateDTO.class))
            )
            @RequestBody @Validated GroupCreateDTO dto) {
        Long userId = UserContext.getUserId();
        GroupVO groupVO = groupService.createGroup(dto, userId);
        return Result.ok(groupVO);
    }

    @Operation(
            summary = "查询群组详情",
            description = "查询群组的详细信息，包括群基本信息和成员列表。需要是群成员才能查询。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = GroupDetailVO.class))),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2006", description = "群组已解散"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @GetMapping("/{groupId}")
    public Result<GroupDetailVO> getGroupDetail(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        GroupDetailVO groupDetail = groupService.getGroupDetail(groupId, userId);
        return Result.ok(groupDetail);
    }

    @Operation(
            summary = "查询我的群组列表",
            description = "查询当前用户加入的所有群组列表。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    @RequireLogin
    @GetMapping("/my-groups")
    public Result<List<GroupVO>> getMyGroups() {
        Long userId = UserContext.getUserId();
        List<GroupVO> groups = groupService.getMyGroups(userId);
        return Result.ok(groups);
    }

    @Operation(
            summary = "修改群信息",
            description = "修改群名称、群头像、群简介等信息。仅群主有权限操作。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主可修改）")
    })
    @RequireLogin
    @PutMapping("/{groupId}")
    public Result<Void> updateGroup(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "修改群信息请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupUpdateDTO.class))
            )
            @RequestBody @Validated GroupUpdateDTO dto) {
        Long userId = UserContext.getUserId();
        groupService.updateGroup(groupId, dto, userId);
        return Result.ok();
    }

    @Operation(
            summary = "解散群组",
            description = "解散群组，群组解散后所有成员将退出。仅群主有权限操作。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "解散成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主可解散）")
    })
    @RequireLogin
    @DeleteMapping("/{groupId}")
    public Result<Void> disbandGroup(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        groupService.disbandGroup(groupId, userId);
        return Result.ok();
    }
}

