package com.wreckloud.wolfchat.chat.media.api.controller;

import com.wreckloud.wolfchat.chat.media.api.dto.ApplyChatUploadPolicyDTO;
import com.wreckloud.wolfchat.chat.media.application.service.ChatMediaService;
import com.wreckloud.wolfchat.common.excption.BaseException;
import com.wreckloud.wolfchat.common.excption.ErrorCode;
import com.wreckloud.wolfchat.common.security.context.UserContext;
import com.wreckloud.wolfchat.common.storage.model.MediaUploadPolicy;
import com.wreckloud.wolfchat.common.storage.service.MediaStorageService;
import com.wreckloud.wolfchat.common.web.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description 聊天媒体控制器
 * @Author Wreckloud
 * @Date 2026-03-09
 */
@Tag(name = "聊天-媒体", description = "聊天媒体上传相关接口")
@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Slf4j
public class ChatMediaController {
    private static final Pattern BYTE_RANGE_PATTERN = Pattern.compile("bytes=(\\d*)-(\\d*)");

    private final ChatMediaService chatMediaService;
    private final MediaStorageService mediaStorageService;

    @Operation(summary = "申请图片上传策略", description = "申请聊天图片本地直传的表单策略")
    @PostMapping("/chat/image/upload-policy")
    public Result<MediaUploadPolicy> applyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请视频上传策略", description = "申请聊天视频本地直传的表单策略")
    @PostMapping("/chat/video/upload-policy")
    public Result<MediaUploadPolicy> applyVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请文件上传策略", description = "申请聊天文件本地直传的表单策略")
    @PostMapping("/chat/file/upload-policy")
    public Result<MediaUploadPolicy> applyFileUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createFileUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题图片上传策略", description = "申请论坛主题图片本地直传的表单策略")
    @PostMapping("/forum/thread/image/upload-policy")
    public Result<MediaUploadPolicy> applyForumThreadImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛主题视频上传策略", description = "申请论坛主题视频本地直传的表单策略")
    @PostMapping("/forum/thread/video/upload-policy")
    public Result<MediaUploadPolicy> applyForumThreadVideoUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        return Result.success(chatMediaService.createForumThreadVideoUploadPolicy(userId, dto));
    }

    @Operation(summary = "申请论坛回复图片上传策略", description = "申请论坛回复图片本地直传的表单策略")
    @PostMapping("/forum/reply/image/upload-policy")
    public Result<MediaUploadPolicy> applyForumReplyImageUploadPolicy(@RequestBody @Validated ApplyChatUploadPolicyDTO dto) {
        Long userId = UserContext.getRequiredUserId();
        log.info("申请论坛回帖图片上传策略: userId={}, extension={}, size={}", userId, dto.getExtension(), dto.getSize());
        return Result.success(chatMediaService.createForumReplyImageUploadPolicy(userId, dto));
    }

    @Operation(summary = "上传媒体文件", description = "使用 upload-policy 返回的参数上传媒体到本地存储")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<String> uploadMedia(@RequestPart("file") MultipartFile file,
                                      @RequestParam("key") String objectKey,
                                      @RequestParam("signature") String signature) {
        mediaStorageService.storeUploadedObject(objectKey, signature, file);
        return Result.success(objectKey);
    }

    @Operation(summary = "读取媒体文件", description = "使用签名链接读取本地媒体文件")
    @GetMapping("/object")
    public ResponseEntity<?> readMedia(@RequestParam("key") String objectKey,
                                       @RequestParam("expires") Long expires,
                                       @RequestParam("signature") String signature,
                                       @RequestParam(value = "process", required = false) String process,
                                       @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        try {
            MediaStorageService.StoredObject storedObject = mediaStorageService.resolveSignedReadableObject(
                    objectKey,
                    expires,
                    signature,
                    process
            );
            Path filePath = storedObject.getFilePath();
            FileSystemResource resource = new FileSystemResource(filePath);
            MediaType contentType = resolveContentType(storedObject.getContentType());
            long fileSize = storedObject.getFileSize();

            ResponseEntity<?> partialResponse = buildPartialResponseIfRequested(filePath, contentType, fileSize, rangeHeader);
            if (partialResponse != null) {
                return partialResponse;
            }

            return ResponseEntity.ok()
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .contentType(contentType)
                    .contentLength(fileSize)
                    .body(resource);
        } catch (BaseException e) {
            HttpStatus status = resolveBusinessStatus(e.getCode());
            log.warn(
                    "读取媒体失败: key={}, uri=/api/media/object, code={}, message={}",
                    objectKey,
                    e.getCode(),
                    e.getMessage()
            );
            return ResponseEntity.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error(e.getCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("读取媒体异常: key={}, uri=/api/media/object", objectKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Result.error(ErrorCode.SYSTEM_ERROR));
        }
    }

    private HttpStatus resolveBusinessStatus(Integer code) {
        if (code == null) {
            return HttpStatus.BAD_REQUEST;
        }
        if (ErrorCode.UNAUTHORIZED.getCode().equals(code) || ErrorCode.TOKEN_INVALID.getCode().equals(code)) {
            return HttpStatus.UNAUTHORIZED;
        }
        return HttpStatus.BAD_REQUEST;
    }

    private MediaType resolveContentType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private ResponseEntity<?> buildPartialResponseIfRequested(Path filePath,
                                                              MediaType contentType,
                                                              long fileSize,
                                                              String rangeHeader) {
        if (!StringUtils.hasText(rangeHeader) || fileSize <= 0) {
            return null;
        }
        ByteRange byteRange = parseByteRange(rangeHeader, fileSize);
        if (byteRange == null) {
            return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                    .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                    .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                    .build();
        }

        long contentLength = byteRange.getLength();
        InputStreamResource partialResource;
        try {
            InputStream inputStream = Files.newInputStream(filePath);
            if (!skipFully(inputStream, byteRange.getStart())) {
                inputStream.close();
                return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
                        .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                        .header(HttpHeaders.CONTENT_RANGE, "bytes */" + fileSize)
                        .build();
            }
            partialResource = new InputStreamResource(new LimitedInputStream(inputStream, contentLength));
        } catch (IOException e) {
            log.error("读取媒体分片失败: path={}, range={}", filePath, rangeHeader, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }

        return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_RANGE, "bytes " + byteRange.getStart() + "-" + byteRange.getEnd() + "/" + fileSize)
                .contentType(contentType)
                .contentLength(contentLength)
                .body(partialResource);
    }

    private ByteRange parseByteRange(String rangeHeader, long fileSize) {
        Matcher matcher = BYTE_RANGE_PATTERN.matcher(rangeHeader.trim());
        if (!matcher.matches()) {
            return null;
        }
        String startGroup = matcher.group(1);
        String endGroup = matcher.group(2);
        if (!StringUtils.hasText(startGroup) && !StringUtils.hasText(endGroup)) {
            return null;
        }

        long start;
        long end;
        if (!StringUtils.hasText(startGroup)) {
            long suffixLength = parsePositiveLong(endGroup, -1L);
            if (suffixLength <= 0) {
                return null;
            }
            if (suffixLength >= fileSize) {
                start = 0;
            } else {
                start = fileSize - suffixLength;
            }
            end = fileSize - 1L;
        } else {
            start = parsePositiveLong(startGroup, -1L);
            if (start < 0 || start >= fileSize) {
                return null;
            }
            if (StringUtils.hasText(endGroup)) {
                end = parsePositiveLong(endGroup, -1L);
                if (end < start) {
                    return null;
                }
                if (end >= fileSize) {
                    end = fileSize - 1L;
                }
            } else {
                end = fileSize - 1L;
            }
        }
        return new ByteRange(start, end);
    }

    private long parsePositiveLong(String value, long defaultValue) {
        try {
            return Long.parseLong(value);
        } catch (Exception ex) {
            return defaultValue;
        }
    }

    private boolean skipFully(InputStream inputStream, long totalToSkip) throws IOException {
        long remaining = totalToSkip;
        while (remaining > 0) {
            long skipped = inputStream.skip(remaining);
            if (skipped > 0) {
                remaining -= skipped;
                continue;
            }
            int fallback = inputStream.read();
            if (fallback == -1) {
                return false;
            }
            remaining--;
        }
        return true;
    }

    @lombok.Getter
    @lombok.RequiredArgsConstructor
    private static class ByteRange {
        private final long start;
        private final long end;

        public long getLength() {
            return end - start + 1L;
        }
    }

    private static class LimitedInputStream extends FilterInputStream {
        private long remaining;

        protected LimitedInputStream(InputStream in, long maxLength) {
            super(in);
            this.remaining = Math.max(0L, maxLength);
        }

        @Override
        public int read() throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int value = super.read();
            if (value != -1) {
                remaining--;
            }
            return value;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (remaining <= 0) {
                return -1;
            }
            int maxLen = (int) Math.min(len, remaining);
            int count = super.read(b, off, maxLen);
            if (count > 0) {
                remaining -= count;
            }
            return count;
        }
    }
}
