package com.wreckloud.wolfchat.community.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 保存主题草稿 DTO
 */
@Data
@Schema(description = "保存主题草稿请求")
public class SaveThreadDraftDTO {
    @Schema(description = "草稿主题ID（存在则更新，不传则创建）")
    private Long threadId;

    @Size(max = 120, message = "主题标题长度不能超过120个字符")
    @Schema(description = "主题标题，可为空")
    private String title;

    @Size(max = 5000, message = "主题内容长度不能超过5000个字符")
    @Schema(description = "主题内容，可为空")
    private String content;

    @Size(max = 9, message = "图片数量不能超过9张")
    @Schema(description = "主题图片对象 Key 列表，最多9张")
    private List<@NotBlank(message = "图片对象不能为空") String> imageKeys;

    @Size(max = 255, message = "视频对象长度不能超过255个字符")
    @Schema(description = "主题视频对象 Key，图片与视频二选一")
    private String videoKey;

    @Size(max = 255, message = "视频封面对象长度不能超过255个字符")
    @Schema(description = "主题视频封面对象 Key，仅视频草稿可选")
    private String videoPosterKey;
}

