package org.example.springboot.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.example.springboot.entity.Tag;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "文章响应DTO")
public class ArticleResponseDTO {
    
    @Schema(description = "文章ID")
    private String id;
    
    @Schema(description = "文章标题")
    private String title;
    
    @Schema(description = "文章内容(Markdown格式)")
    private String content;
    
    @Schema(description = "文章内容(HTML格式)")
    private String htmlContent;
    
    @Schema(description = "文章摘要")
    private String summary;

    @Schema(description = "文章大纲(JSON格式)")
    private String outline;

    @Schema(description = "封面图片")
    private String coverImage;

    @Schema(description = "文章主色调(十六进制颜色值)")
    private String mainColor;
    
    @Schema(description = "分类ID")
    private Long categoryId;
    
    @Schema(description = "作者ID")
    private Long userId;
    
    @Schema(description = "浏览量")
    private Integer viewCount;
    
    @Schema(description = "点赞数")
    private Integer likeCount;
    
    @Schema(description = "评论数")
    private Integer commentCount;
    
    @Schema(description = "收藏数")
    private Integer collectCount;
    
    @Schema(description = "状态(0:草稿,1:已发布,2:已删除)")
    private Integer status;
    
    @Schema(description = "创建时间")
    private LocalDateTime createTime;
    
    @Schema(description = "更新时间")
    private LocalDateTime updateTime;
    
    @Schema(description = "是否置顶(0:否,1:是)")
    private Integer isTop;
    
    @Schema(description = "是否推荐(0:否,1:是)")
    private Integer isRecommend;
    
    @Schema(description = "是否开启密码保护(0:否,1:是)")
    private Integer isPasswordProtected;
    
    @Schema(description = "密码过期时间(NULL表示永不过期)")
    private LocalDateTime passwordExpireTime;
    
    @Schema(description = "是否已向量化(0:否,1:是)")
    private Integer isVectorized;
    
    @Schema(description = "向量化状态(0:未向量化,1:处理中,2:成功,-1:失败)")
    private Integer vectorizedStatus;
    
    @Schema(description = "向量化失败原因")
    private String vectorizedErrorReason;
    
    @Schema(description = "最后成功向量化时间")
    private LocalDateTime lastVectorizedSuccessTime;
    
    @Schema(description = "向量化处理开始时间")
    private LocalDateTime vectorizedProcessAt;
    
    @Schema(description = "向量化重试次数")
    private Integer vectorizedRetryCount;
    
    @Schema(description = "分类名称")
    private String categoryName;
    
    @Schema(description = "作者名称")
    private String authorName;
    
    @Schema(description = "标签列表")
    private List<Tag> tags;
}
