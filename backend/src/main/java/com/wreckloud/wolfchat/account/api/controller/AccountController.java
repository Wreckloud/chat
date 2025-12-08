package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.MobileLoginDTO;
import com.wreckloud.wolfchat.account.api.dto.MobileRegisterDTO;
import com.wreckloud.wolfchat.account.api.dto.WechatLoginDTO;
import com.wreckloud.wolfchat.account.api.vo.CaptchaVO;
import com.wreckloud.wolfchat.account.api.vo.SmsCodeVO;
import com.wreckloud.wolfchat.account.api.vo.UserVO;
import com.wreckloud.wolfchat.account.application.service.AccountService;
import com.wreckloud.wolfchat.account.application.service.CaptchaService;
import com.wreckloud.wolfchat.account.application.service.SmsService;
import com.wreckloud.wolfchat.account.domain.entity.WfUser;
import com.wreckloud.wolfchat.account.infra.mapper.WfUserMapper;
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

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/**
 * @Description 账户控制器
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Tag(name = "账户管理", description = "账户相关接口，包括注册、查询等")
@RestController
@RequestMapping("/account")
@Validated
public class AccountController {

    @Autowired
    private WfUserMapper wfUserMapper;

    @Autowired
    private CaptchaService captchaService;

    @Autowired
    private AccountService accountService;

    @Autowired
    private SmsService smsService;

    @Operation(
            summary = "获取验证码",
            description = "生成图形验证码，返回验证码key和Base64编码的图片。验证码有效期为5分钟，使用后自动删除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = CaptchaVO.class))),
            @ApiResponse(responseCode = "500", description = "系统异常")
    })
    @GetMapping("/captcha")
    public Result<CaptchaVO> generateCaptcha() {
        CaptchaVO captchaVO = captchaService.generateCaptcha();
        return Result.ok(captchaVO);
    }

    @Operation(
            summary = "手机号注册",
            description = "通过手机号注册新用户。注册流程：1. 获取验证码 2. 填写手机号、密码等信息 3. 提交注册。验证码验证在注册接口中完成，无需单独验证。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "注册成功"),
            @ApiResponse(responseCode = "-1001", description = "手机号已被注册"),
            @ApiResponse(responseCode = "-1002", description = "验证码错误或已过期"),
            @ApiResponse(responseCode = "-1003", description = "验证码不能为空"),
            @ApiResponse(responseCode = "-1004", description = "暂无可用号码，请稍后再试"),
            @ApiResponse(responseCode = "-1005", description = "两次输入的密码不一致"),
            @ApiResponse(responseCode = "-1000", description = "参数校验失败")
    })
    @PostMapping("/register/mobile")
    public Result<Void> registerByMobile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "注册请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MobileRegisterDTO.class))
            )
            @RequestBody @Validated MobileRegisterDTO request) {
        accountService.registerByMobile(request);
        return Result.ok();
    }

    @Operation(
            summary = "发送短信验证码",
            description = "向指定手机号发送6位数字验证码。验证码有效期为5分钟，60秒内不能重复发送。开发环境会打印到日志，生产环境需要配置真实的短信服务。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "发送成功", content = @Content(schema = @Schema(implementation = SmsCodeVO.class))),
            @ApiResponse(responseCode = "-1014", description = "发送过于频繁，请稍后再试"),
            @ApiResponse(responseCode = "-1000", description = "参数校验失败")
    })
    @PostMapping("/sms/send")
    public Result<SmsCodeVO> sendSmsCode(
            @Parameter(description = "手机号", required = true, example = "13800138000")
            @RequestParam @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1\\d{10}$", message = "手机号格式不正确") String mobile) {
        String smsCodeKey = smsService.sendSmsCode(mobile);
        SmsCodeVO vo = new SmsCodeVO(smsCodeKey, "验证码已发送，请查收短信（开发环境请查看日志）");
        return Result.ok(vo);
    }

    @Operation(
            summary = "手机号验证码登录/注册",
            description = "通过手机号和验证码登录。如果用户已存在则直接登录，不存在则自动注册。验证码验证通过后自动删除。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = UserVO.class))),
            @ApiResponse(responseCode = "-1013", description = "短信验证码错误或已过期"),
            @ApiResponse(responseCode = "-1004", description = "暂无可用号码，请稍后再试"),
            @ApiResponse(responseCode = "-1012", description = "用户已被禁用或注销"),
            @ApiResponse(responseCode = "-1000", description = "参数校验失败")
    })
    @PostMapping("/login/mobile")
    public Result<UserVO> loginByMobile(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "手机号登录请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = MobileLoginDTO.class))
            )
            @RequestBody @Validated MobileLoginDTO request) {
        UserVO userVO = accountService.loginByMobile(request);
        return Result.ok(userVO);
    }

    @Operation(
            summary = "微信一键登录/注册",
            description = "通过微信openid和unionid登录。如果用户已存在则直接登录，不存在则自动注册。支持更新用户昵称、头像等信息。"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "登录成功", content = @Content(schema = @Schema(implementation = UserVO.class))),
            @ApiResponse(responseCode = "-1004", description = "暂无可用号码，请稍后再试"),
            @ApiResponse(responseCode = "-1012", description = "用户已被禁用或注销"),
            @ApiResponse(responseCode = "-1000", description = "参数校验失败")
    })
    @PostMapping("/login/wechat")
    public Result<UserVO> loginByWechat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "微信登录请求参数",
                    required = true,
                    content = @Content(schema = @Schema(implementation = WechatLoginDTO.class))
            )
            @RequestBody @Validated WechatLoginDTO request) {
        UserVO userVO = accountService.loginByWechat(request);
        return Result.ok(userVO);
    }

    @Operation(
            summary = "根据ID查询用户",
            description = "通过用户ID查询用户详细信息，仅用于测试"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功", content = @Content(schema = @Schema(implementation = WfUser.class))),
            @ApiResponse(responseCode = "500", description = "系统异常")
    })
    @GetMapping("/user/{id}")
    public Result<WfUser> getUserById(
            @Parameter(description = "用户ID", required = true, example = "1")
            @PathVariable Long id) {
        WfUser user = wfUserMapper.selectById(id);
        return Result.ok(user);
    }
}

