package com.wreckloud.wolfchat.group.api.controller;

import com.wreckloud.wolfchat.common.security.annotation.RequireLogin;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.web.Result;
import com.wreckloud.wolfchat.group.api.dto.GroupMemberInviteDTO;
import com.wreckloud.wolfchat.group.api.dto.GroupMemberRoleDTO;
import com.wreckloud.wolfchat.group.api.vo.GroupMemberVO;
import com.wreckloud.wolfchat.group.application.service.GroupMemberService;
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * @Description 群成员管理控制器
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Tag(name = "群成员管理", description = "群成员相关接口，包括邀请、踢出、退出群组等")
@RestController
@RequestMapping("/group/member")
@Validated
public class GroupMemberController {

    @Autowired
    private GroupMemberService groupMemberService;

    @Operation(
            summary = "邀请成员入群",
            description = "邀请好友加入群组。所有群成员都可以邀请，但需要检查群人数是否已满。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "邀请成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2002", description = "群成员已满"),
            @ApiResponse(responseCode = "-2003", description = "已经在群内"),
            @ApiResponse(responseCode = "-2006", description = "群组已解散"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PostMapping("/{groupId}/invite")
    public Result<Void> inviteMembers(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "邀请成员请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupMemberInviteDTO.class))
            )
            @RequestBody @Validated GroupMemberInviteDTO dto) {
        Long userId = UserContext.getUserId();
        groupMemberService.inviteMembers(groupId, dto.getUserIds(), userId);
        return Result.ok();
    }

    @Operation(
            summary = "踢出成员",
            description = "将成员踢出群组。群主可以踢出任何人（除了自己），管理员只能踢出普通成员。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "踢出成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内"),
            @ApiResponse(responseCode = "-2008", description = "不能踢出群主"),
            @ApiResponse(responseCode = "-2009", description = "管理员不能踢出其他管理员")
    })
    @RequireLogin
    @DeleteMapping("/{groupId}/member/{targetUserId}")
    public Result<Void> kickMember(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @Parameter(description = "被踢出用户ID", required = true, example = "2")
            @PathVariable Long targetUserId) {
        Long userId = UserContext.getUserId();
        groupMemberService.kickMember(groupId, targetUserId, userId);
        return Result.ok();
    }

    @Operation(
            summary = "退出群组",
            description = "主动退出群组。群主不能退出，只能转让群主或解散群组。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "退出成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2005", description = "群主不能退出群组"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PostMapping("/{groupId}/quit")
    public Result<Void> quitGroup(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        groupMemberService.quitGroup(groupId, userId);
        return Result.ok();
    }

    @Operation(
            summary = "设置/取消管理员",
            description = "设置或取消群成员的管理员权限。仅群主有权限操作。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "设置成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2004", description = "无权限操作（仅群主可设置）"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PostMapping("/{groupId}/set-admin")
    public Result<Void> setAdmin(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "设置管理员请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = GroupMemberRoleDTO.class))
            )
            @RequestBody @Validated GroupMemberRoleDTO dto) {
        Long userId = UserContext.getUserId();
        groupMemberService.setAdmin(groupId, dto.getUserId(), dto.getIsAdmin(), userId);
        return Result.ok();
    }

    @Operation(
            summary = "查询群成员列表",
            description = "查询群组的所有成员。按角色和加入时间排序（群主、管理员、普通成员）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内（无权查看）")
    })
    @RequireLogin
    @GetMapping("/{groupId}/members")
    public Result<List<GroupMemberVO>> getMembers(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId) {
        Long userId = UserContext.getUserId();
        List<GroupMemberVO> members = groupMemberService.getMembers(groupId, userId);
        return Result.ok(members);
    }

    @Operation(
            summary = "修改群昵称",
            description = "修改自己在群内显示的昵称（群名片）。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "修改成功"),
            @ApiResponse(responseCode = "-2001", description = "群组不存在"),
            @ApiResponse(responseCode = "-2007", description = "该用户不在群内")
    })
    @RequireLogin
    @PutMapping("/{groupId}/nickname")
    public Result<Void> updateNickname(
            @Parameter(description = "群组ID", required = true, example = "1")
            @PathVariable Long groupId,
            @Parameter(description = "新的群昵称", required = true, example = "班长")
            @RequestParam @NotBlank(message = "群昵称不能为空") @Size(max = 50, message = "群昵称不能超过50个字符") String nickname) {
        Long userId = UserContext.getUserId();
        groupMemberService.updateNickname(groupId, userId, nickname);
        return Result.ok();
    }
}

