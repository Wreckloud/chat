package com.wreckloud.wolfchat.account.api.controller;

import com.wreckloud.wolfchat.account.api.dto.LoginDTO;
import com.wreckloud.wolfchat.account.api.dto.RegisterDTO;
import com.wreckloud.wolfchat.account.api.dto.SendResetPasswordLinkDTO;
import com.wreckloud.wolfchat.account.api.vo.LoginVO;
import com.wreckloud.wolfchat.account.application.service.AuthService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * @Description 认证控制器
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@Tag(name = "认证", description = "认证相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 注册
     * 用户输入行者名、密码、可选邮箱，系统分配狼藉号并注册
     */
    @Operation(summary = "注册", description = "输入行者名、密码、可选邮箱，系统分配狼藉号并注册")
    @PostMapping("/register")
    public Result<LoginVO> register(@RequestBody @Validated RegisterDTO dto) {
        LoginVO loginVO = authService.register(dto.getNickname(), dto.getPassword(), dto.getEmail());
        return Result.success("注册成功", loginVO);
    }

    /**
     * 账号+密码登录（账号支持狼藉号或邮箱）
     */
    @Operation(summary = "登录", description = "使用账号（狼藉号或邮箱）+密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated LoginDTO dto, HttpServletRequest request) {
        try {
            LoginVO loginVO = authService.login(dto.getAccount(), dto.getLoginKey(), request);
            return Result.success("登录成功", loginVO);
        } catch (BaseException e) {
            log.debug("登录失败: code={}", e.getCode());
            return Result.error(ErrorCode.LOGIN_FAILED);
        } catch (Exception e) {
            log.error("登录异常", e);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
    }

    /**
     * 发送重置密码邮箱链接
     */
    @Operation(summary = "发送重置密码链接", description = "发送重置密码邮箱链接")
    @PostMapping("/password/reset-link/send")
    public Result<Void> sendResetPasswordLink(@RequestBody @Validated SendResetPasswordLinkDTO dto) {
        authService.sendResetPasswordLink(dto.getEmail());
        return Result.success("重置链接已发送", null);
    }

    /**
     * 重置密码页面
     */
    @Operation(summary = "重置密码页面", description = "打开邮件链接后展示重置密码表单")
    @GetMapping(value = "/password/reset", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> showResetPasswordPage(@RequestParam("token") String token) {
        if (!StringUtils.hasText(token)) {
            return ResponseEntity.badRequest().body(buildResultPage(false, "重置失败", ErrorCode.PASSWORD_RESET_LINK_INVALID.getMessage()));
        }
        if (!authService.isResetPasswordTokenAvailable(token)) {
            return ResponseEntity.badRequest().body(buildResultPage(false, "重置失败", ErrorCode.PASSWORD_RESET_LINK_INVALID.getMessage()));
        }
        return ResponseEntity.ok(buildResetPasswordFormPage(token, null));
    }

    /**
     * 提交重置密码
     */
    @Operation(summary = "提交重置密码", description = "通过链接 token 提交新密码")
    @PostMapping(value = "/password/reset", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> submitResetPassword(
            @RequestParam("token") String token,
            @RequestParam("newLoginKey") String newLoginKey,
            @RequestParam("confirmLoginKey") String confirmLoginKey
    ) {
        try {
            authService.resetPasswordByToken(token, newLoginKey, confirmLoginKey);
            return ResponseEntity.ok(buildResultPage(true, "密码重置成功", "密码已更新，请返回小程序使用新密码登录。"));
        } catch (BaseException e) {
            log.debug("重置密码失败: code={}", e.getCode());
            if (isRetryableResetError(e)) {
                return ResponseEntity.badRequest().body(buildResetPasswordFormPage(token, e.getMessage()));
            }
            return ResponseEntity.badRequest().body(buildResultPage(false, "密码重置失败", e.getMessage()));
        } catch (Exception e) {
            log.error("重置密码异常", e);
            return ResponseEntity.internalServerError()
                    .body(buildResultPage(false, "密码重置失败", ErrorCode.SYSTEM_ERROR.getMessage()));
        }
    }

    /**
     * 邮箱认证链接回调
     */
    @Operation(summary = "邮箱认证回调", description = "点击邮件中的认证链接后调用，完成邮箱认证")
    @GetMapping(value = "/email/verify", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmailByLink(@RequestParam("token") String token) {
        try {
            authService.verifyBindEmailByToken(token);
            return ResponseEntity.ok(buildResultPage(true, "邮箱认证成功", "邮箱已认证，现在可以使用邮箱 + 密码登录。"));
        } catch (BaseException e) {
            log.debug("邮箱认证失败: code={}", e.getCode());
            return ResponseEntity.badRequest().body(buildResultPage(false, "邮箱认证失败", e.getMessage()));
        } catch (Exception e) {
            log.error("邮箱认证异常", e);
            return ResponseEntity.internalServerError()
                    .body(buildResultPage(false, "邮箱认证失败", ErrorCode.SYSTEM_ERROR.getMessage()));
        }
    }

    private boolean isRetryableResetError(BaseException e) {
        Integer code = e.getCode();
        return ErrorCode.PARAM_ERROR.getCode().equals(code)
                || ErrorCode.NEW_LOGIN_KEY_NOT_MATCH.getCode().equals(code);
    }

    private String buildResetPasswordFormPage(String token, String errorMessage) {
        String errorHtml = StringUtils.hasText(errorMessage)
                ? "<div class='alert error'>" + escapeHtml(errorMessage) + "</div>"
                : "";

        return "<!doctype html>" +
                "<html lang='zh-CN'>" +
                "<head>" +
                "<meta charset='UTF-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
                "<title>WolfChat 重置密码</title>" +
                "<style>" +
                "body{margin:0;background:#dbe7f6;color:#20324a;font-family:Tahoma,'Microsoft YaHei',sans-serif;}" +
                ".wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;}" +
                ".card{width:min(560px,100%);background:#f8fbff;border:2px solid #365f8f;box-shadow:6px 6px 0 #9db6d3;padding:24px;}" +
                ".title{margin:0 0 10px;font-size:24px;letter-spacing:1px;}" +
                ".desc{margin:0 0 18px;font-size:14px;line-height:1.7;color:#48627e;}" +
                ".field{margin-bottom:14px;}" +
                ".label{display:block;margin-bottom:6px;font-size:13px;color:#4e6079;}" +
                ".input{width:100%;height:42px;border:1px solid #93a5bc;background:#fff;padding:0 12px;font-size:15px;box-sizing:border-box;}" +
                ".btn{width:100%;height:42px;border:1px solid #2f5c9b;background:linear-gradient(180deg,#4a74b3 0%,#2f5c9b 100%);color:#fff;font-size:15px;cursor:pointer;}" +
                ".alert{margin-bottom:14px;padding:10px;border:1px solid #9caec5;background:#eef4fc;font-size:13px;}" +
                ".alert.error{border-color:#9a4a52;background:#f8ecee;color:#7a2f36;}" +
                "</style>" +
                "</head>" +
                "<body><div class='wrap'><div class='card'>" +
                "<h1 class='title'>重置密码</h1>" +
                "<p class='desc'>请设置新的登录密码（6-64位）。</p>" +
                errorHtml +
                "<form method='post' action='/api/auth/password/reset'>" +
                "<input type='hidden' name='token' value='" + escapeHtml(token) + "'/>" +
                "<div class='field'><label class='label'>新密码</label><input class='input' type='password' name='newLoginKey' minlength='6' maxlength='64' required/></div>" +
                "<div class='field'><label class='label'>确认密码</label><input class='input' type='password' name='confirmLoginKey' minlength='6' maxlength='64' required/></div>" +
                "<button class='btn' type='submit'>确认重置</button>" +
                "</form></div></div></body></html>";
    }

    private String buildResultPage(boolean success, String title, String message) {
        String badgeClass = success ? "badge success" : "badge error";
        String cardClass = success ? "card success" : "card error";
        return "<!doctype html>" +
                "<html lang='zh-CN'>" +
                "<head>" +
                "<meta charset='UTF-8'/>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'/>" +
                "<title>WolfChat</title>" +
                "<style>" +
                "body{margin:0;background:#dbe7f6;color:#20324a;font-family:Tahoma,'Microsoft YaHei',sans-serif;}" +
                ".wrap{min-height:100vh;display:flex;align-items:center;justify-content:center;padding:24px;}" +
                ".card{width:min(520px,100%);background:#f8fbff;border:2px solid #365f8f;box-shadow:6px 6px 0 #9db6d3;padding:24px;}" +
                ".card.success{border-color:#2d6a4f;box-shadow:6px 6px 0 #9bc9b2;}" +
                ".card.error{border-color:#8f3e36;box-shadow:6px 6px 0 #d2aaa5;}" +
                ".title{margin:0 0 12px;font-size:22px;letter-spacing:1px;}" +
                ".desc{margin:0 0 18px;font-size:14px;line-height:1.7;}" +
                ".badge{display:inline-block;font-size:12px;padding:4px 10px;border:1px solid #365f8f;background:#e4eef9;}" +
                ".badge.success{border-color:#2d6a4f;background:#e4f3ec;color:#224f3b;}" +
                ".badge.error{border-color:#8f3e36;background:#f8eaea;color:#732f29;}" +
                "</style>" +
                "</head>" +
                "<body><div class='wrap'><div class='" + cardClass + "'>" +
                "<h1 class='title'>" + escapeHtml(title) + "</h1>" +
                "<p class='desc'>" + escapeHtml(message) + "</p>" +
                "<span class='" + badgeClass + "'>WolfChat</span>" +
                "</div></div></body></html>";
    }

    private String escapeHtml(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
