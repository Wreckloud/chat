package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.NoPoolAddDTO;
import com.wreckloud.wolfchat.account.api.dto.NoPoolGenerateDTO;
import com.wreckloud.wolfchat.account.api.dto.NoPoolQueryDTO;
import com.wreckloud.wolfchat.account.api.dto.NoPoolUpdateDTO;
import com.wreckloud.wolfchat.account.application.service.NoPoolService;
import com.wreckloud.wolfchat.account.domain.entity.WfNoPool;
import com.wreckloud.wolfchat.common.web.PageResult;
import com.wreckloud.wolfchat.common.web.Result;
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

/**
 * @Description 号码池管理控制器
 * @Author Wreckloud
 * @Date 2025-12-07
 */
@Tag(name = "号码池管理", description = "号码池相关接口，包括增删改查、自动生成等")
@RestController
@RequestMapping("/account/nopool")
public class NoPoolController {

    @Autowired
    private NoPoolService noPoolService;

    @Operation(
            summary = "自动生成普通号码",
            description = "自动生成指定数量的普通号码（从1000000开始，非顺序递增）。每次生成的号码都是随机的，确保非顺序性。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "生成成功"),
            @ApiResponse(responseCode = "-1006", description = "参数无效"),
            @ApiResponse(responseCode = "-1009", description = "号码生成失败，可能号码池接近饱和")
    })
    @PostMapping("/generate")
    public Result<Integer> generateNumbers(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "生成请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NoPoolGenerateDTO.class))
            )
            @RequestBody @Validated NoPoolGenerateDTO request) {
        int count = noPoolService.generateNumbers(request.getCount());
        return Result.ok(count);
    }

    @Operation(
            summary = "管理员手动添加号码",
            description = "管理员手动添加指定号码到号码池。可以是普通号或靓号，只要号码未被占用即可添加。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "添加成功"),
            @ApiResponse(responseCode = "-1007", description = "号码格式无效"),
            @ApiResponse(responseCode = "-1008", description = "号码已存在")
    })
    @PostMapping("/add")
    public Result<Void> addNumber(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "添加号码请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NoPoolAddDTO.class))
            )
            @RequestBody @Validated NoPoolAddDTO request) {
        noPoolService.addNumber(request.getWfNo(), request.getIsPretty());
        return Result.ok();
    }

    @Operation(
            summary = "分页查询号码池",
            description = "支持按号码、状态、是否靓号、用户ID等条件查询，支持分页"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = PageResult.class)))
    })
    @GetMapping("/page")
    public Result<PageResult<WfNoPool>> queryPage(NoPoolQueryDTO queryDTO) {
        // 设置默认值
        if (queryDTO.getPage() == null || queryDTO.getPage() < 1) {
            queryDTO.setPage(1);
        }
        if (queryDTO.getSize() == null || queryDTO.getSize() < 1) {
            queryDTO.setSize(10);
        }
        PageResult<WfNoPool> result = noPoolService.queryPage(queryDTO);
        return Result.ok(result);
    }

    @Operation(
            summary = "根据ID查询号码",
            description = "通过号码ID查询详细信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = WfNoPool.class))),
            @ApiResponse(responseCode = "-1010", description = "号码不存在")
    })
    @GetMapping("/{id}")
    public Result<WfNoPool> getById(
            @Parameter(description = "号码ID", required = true, example = "1")
            @PathVariable Long id) {
        WfNoPool noPool = noPoolService.getById(id);
        return Result.ok(noPool);
    }

    @Operation(
            summary = "根据号码查询",
            description = "通过号码查询详细信息"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "查询成功", content = @Content(schema = @Schema(implementation = WfNoPool.class))),
            @ApiResponse(responseCode = "-1010", description = "号码不存在")
    })
    @GetMapping("/wfno/{wfNo}")
    public Result<WfNoPool> getByWfNo(
            @Parameter(description = "号码", required = true, example = "1000001")
            @PathVariable Long wfNo) {
        WfNoPool noPool = noPoolService.getByWfNo(wfNo);
        return Result.ok(noPool);
    }

    @Operation(
            summary = "更新号码信息",
            description = "更新号码的状态、是否靓号、用户ID等信息。可将userId设为null来释放号码。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "-1010", description = "号码不存在"),
            @ApiResponse(responseCode = "-1006", description = "参数无效")
    })
    @PutMapping("/update")
    public Result<Void> update(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "更新请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = NoPoolUpdateDTO.class))
            )
            @RequestBody @Validated NoPoolUpdateDTO request) {
        noPoolService.update(request);
        return Result.ok();
    }

    @Operation(
            summary = "根据ID删除号码",
            description = "删除指定号码。注意：已被使用的号码无法删除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "-1010", description = "号码不存在"),
            @ApiResponse(responseCode = "-1011", description = "号码已被使用，无法删除")
    })
    @DeleteMapping("/{id}")
    public Result<Void> deleteById(
            @Parameter(description = "号码ID", required = true, example = "1")
            @PathVariable Long id) {
        noPoolService.deleteById(id);
        return Result.ok();
    }

    @Operation(
            summary = "根据号码删除",
            description = "通过号码删除。注意：已被使用的号码无法删除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "-1010", description = "号码不存在"),
            @ApiResponse(responseCode = "-1011", description = "号码已被使用，无法删除")
    })
    @DeleteMapping("/wfno/{wfNo}")
    public Result<Void> deleteByWfNo(
            @Parameter(description = "号码", required = true, example = "1000001")
            @PathVariable Long wfNo) {
        noPoolService.deleteByWfNo(wfNo);
        return Result.ok();
    }
}

