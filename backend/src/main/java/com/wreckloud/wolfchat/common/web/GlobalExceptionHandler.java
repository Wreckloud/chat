package com.wreckloud.wolfchat.common.web;

import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description 全局异常处理器
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String LOGIN_PATH = "/auth/login";
    private static final Pattern MAX_UPLOAD_BYTES_PATTERN =
            Pattern.compile("maximum permitted size of\\s+(\\d+)\\s+bytes", Pattern.CASE_INSENSITIVE);

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.warn("登录业务异常: code={}, uri={}", e.getCode(), resolveRequestUri(request));
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("业务异常: code={}, message={}, uri={}", e.getCode(), e.getMessage(), resolveRequestUri(request));
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数相关异常
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public Result<?> handleParamException(Exception e, HttpServletRequest request) {
        String message = resolveParamErrorMessage(e);
        if (isLoginRequest(request)) {
            log.info("登录参数异常: uri={}, message={}", resolveRequestUri(request), message);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("参数异常: uri={}, message={}", resolveRequestUri(request), message);
        return Result.error(ErrorCode.PARAM_ERROR.getCode(), message);
    }

    /**
     * 处理请求体解析异常
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<?> handleHttpMessageNotReadableException(HttpMessageNotReadableException e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.info("登录请求体异常: uri={}, message={}", resolveRequestUri(request), e.getMessage());
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.warn("请求体异常: uri={}, message={}", resolveRequestUri(request), e.getMessage());
        return Result.error(ErrorCode.PARAM_ERROR);
    }

    /**
     * 处理数据库语法异常
     */
    @ExceptionHandler(BadSqlGrammarException.class)
    public Result<?> handleBadSqlGrammarException(BadSqlGrammarException e, HttpServletRequest request) {
        log.error("数据库结构异常: uri={}", resolveRequestUri(request), e);
        return Result.error(ErrorCode.DATABASE_ERROR);
    }

    /**
     * 处理上传文件超限异常
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e, HttpServletRequest request) {
        long limitBytes = resolveUploadLimitBytes(e);
        String message = limitBytes > 0L
                ? String.format(Locale.ROOT, "文件超过上传大小限制（上限 %s）", formatSize(limitBytes))
                : "文件超过上传大小限制";
        log.warn("上传文件超过大小限制: uri={}, limitBytes={}, message={}",
                resolveRequestUri(request), limitBytes, e.getMessage());
        return Result.error(ErrorCode.MEDIA_FILE_INVALID.getCode(), message);
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e, HttpServletRequest request) {
        if (isLoginRequest(request)) {
            log.error("登录系统异常: uri={}", resolveRequestUri(request), e);
            return Result.error(ErrorCode.LOGIN_FAILED);
        }
        log.error("系统异常: uri={}", resolveRequestUri(request), e);
        return Result.error(ErrorCode.SYSTEM_ERROR);
    }

    private String resolveParamErrorMessage(Exception e) {
        if (e instanceof MethodArgumentNotValidException) {
            MethodArgumentNotValidException ex = (MethodArgumentNotValidException) e;
            if (ex.getBindingResult().hasFieldErrors()) {
                String msg = ex.getBindingResult().getFieldError().getDefaultMessage();
                if (StringUtils.hasText(msg)) {
                    return msg;
                }
            }
        }

        if (e instanceof ConstraintViolationException) {
            ConstraintViolationException ex = (ConstraintViolationException) e;
            if (!ex.getConstraintViolations().isEmpty()) {
                String msg = ex.getConstraintViolations().iterator().next().getMessage();
                if (StringUtils.hasText(msg)) {
                    return msg;
                }
            }
        }

        if (e instanceof IllegalArgumentException && StringUtils.hasText(e.getMessage())) {
            return e.getMessage();
        }

        return ErrorCode.PARAM_ERROR.getMessage();
    }

    private boolean isLoginRequest(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        String contextPath = request.getContextPath();
        String requestUri = request.getRequestURI();
        String loginRequestUri = (contextPath == null ? "" : contextPath) + LOGIN_PATH;
        return loginRequestUri.equals(requestUri);
    }

    private String resolveRequestUri(HttpServletRequest request) {
        if (request == null) {
            return "";
        }
        return request.getRequestURI();
    }

    private long resolveUploadLimitBytes(MaxUploadSizeExceededException e) {
        if (e == null) {
            return 0L;
        }
        long maxSize = e.getMaxUploadSize();
        if (maxSize > 0L) {
            return maxSize;
        }
        String message = e.getMessage();
        if (!StringUtils.hasText(message)) {
            return 0L;
        }
        Matcher matcher = MAX_UPLOAD_BYTES_PATTERN.matcher(message);
        if (!matcher.find()) {
            return 0L;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return 0L;
        }
    }

    private String formatSize(long bytes) {
        if (bytes >= 1024L * 1024L * 1024L) {
            return formatDecimal(bytes / (1024d * 1024d * 1024d), "GB");
        }
        if (bytes >= 1024L * 1024L) {
            return formatDecimal(bytes / (1024d * 1024d), "MB");
        }
        if (bytes >= 1024L) {
            return formatDecimal(bytes / 1024d, "KB");
        }
        return bytes + "B";
    }

    private String formatDecimal(double value, String unit) {
        double rounded = Math.round(value * 10d) / 10d;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0001d) {
            return String.format(Locale.ROOT, "%.0f%s", rounded, unit);
        }
        return String.format(Locale.ROOT, "%.1f%s", rounded, unit);
    }
}
