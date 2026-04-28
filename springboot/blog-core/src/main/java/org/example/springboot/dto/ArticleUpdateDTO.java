package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.example.springboot.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章更新DTO")
public class ArticleUpdateDTO {
    
    @NotBlank(message = "文章标题不能为空")
    @Size(min = 1, max = 200, message = "文章标题长度必须在1到200个字符之间")
    @Schema(description = "文章标题")
    private String title;
    
    @NotBlank(message = "文章内容不能为空")
    @Schema(description = "文章内容(Markdown格式)")
    private String content;
    
    @Schema(description = "文章内容(HTML格式)")
    private String htmlContent;
    
    @Size(max = 500, message = "文章摘要不能超过500个字符")
    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章大纲(JSON格式)")
    private String outline;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "文章主色调(十六进制颜色值)")
    private String mainColor;
    
    @NotNull(message = "分类ID不能为空")
    @Schema(description = "分类ID")
    private Long categoryId;
    
    @Schema(description = "状态(0:草稿,1:已发布,2:已删除)")
    private Integer status;
    
    @Schema(description = "是否置顶(0:否,1:是)")
    private Integer isTop;
    
    @Schema(description = "是否推荐(0:否,1:是)")
    private Integer isRecommend;
    
    @Schema(description = "是否开启密码保护(0:否,1:是)")
    private Integer isPasswordProtected;
    
    @Schema(description = "密码过期时间(NULL表示永不过期)")
    private LocalDateTime passwordExpireTime;
    
    @Schema(description = "标签列表")
    private List<Tag> tags;
}


