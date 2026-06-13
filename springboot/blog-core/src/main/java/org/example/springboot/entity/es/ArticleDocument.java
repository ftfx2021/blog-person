package org.example.springboot.entity.es;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Transient;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.springboot.entity.Tag;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Range;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.convert.DateFormatter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "article_index",createIndex = true)
public class ArticleDocument {

    @TableId(type = IdType.ASSIGN_ID)
    @Schema(description = "文章ID")
    @Id
    private String id;

    @Schema(description = "文章标题")
    @Field(type = FieldType.Text)
    private String title;

    @Schema(description = "文章内容(Markdown格式)")
    @Field(type = FieldType.Text)
    private String content;


    @Schema(description = "文章摘要")
    @Field(type = FieldType.Text)
    private String summary;


    @Schema(description = "作者ID")
    @Field(type = FieldType.Integer)
    private Long userId;

    @Schema(description = "浏览量")
    @Field(type = FieldType.Integer)
    private Integer viewCount;

    @Schema(description = "点赞数")
    @Field(type = FieldType.Integer)
    private Integer likeCount;

    @Schema(description = "评论数")
    @Field(type = FieldType.Integer)
    private Integer commentCount;

    @Schema(description = "状态(0:草稿,1:已发布,2:已删除)")
    @Field(type = FieldType.Integer)
    private Integer status;

    @Schema(description = "创建时间")
    @Field(type = FieldType.Date,format = {}, pattern = {"uuuu-MM-dd HH:mm:ss"})
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") //此注解用来接收字符串类型的参数封装成LocalDateTime类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8", shape = JsonFormat.Shape.STRING) //此注解将date类型数据转成字符串响应出去
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)		// 反序列化
    @JsonSerialize(using = LocalDateTimeSerializer.class)		// 序列化
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") //此注解用来接收字符串类型的参数封装成LocalDateTime类型
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8", shape = JsonFormat.Shape.STRING) //此注解将date类型数据转成字符串响应出去
    @JsonDeserialize(using = LocalDateTimeDeserializer.class)		// 反序列化
    @JsonSerialize(using = LocalDateTimeSerializer.class)		// 序列化
    private LocalDateTime updateTime;


    @Schema(description = "是否推荐(0:否,1:是)")
    private Integer isRecommend;


    @TableField(exist = false)
    @Schema(description = "分类名称")
    private String categoryName;

    @TableField(exist = false)
    @Schema(description = "作者名称")
    private String authorName;

    @TableField(exist = false)
    @Schema(description = "标签列表")
    private List<String> tags;




}
