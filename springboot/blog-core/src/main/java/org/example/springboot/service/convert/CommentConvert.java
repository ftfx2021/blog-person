package org.example.springboot.service.convert;

import org.example.springboot.dto.CommentCreateDTO;
import org.example.springboot.dto.CommentResponseDTO;
import org.example.springboot.entity.Comment;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论转换工具类
 */
public class CommentConvert {
    
    /**
     * DTO 转换为 Entity
     * @param dto 创建DTO
     * @return Entity
     */
    public static Comment toEntity(CommentCreateDTO dto) {
        if (dto == null) {
            return null;
        }
        
        Comment comment = new Comment();
        comment.setContent(dto.getContent());
        comment.setArticleId(dto.getArticleId());
        comment.setParentId(dto.getParentId());
        comment.setToUserId(dto.getToUserId());
        
        return comment;
    }
    
    /**
     * Entity 转换为响应DTO
     * @param comment Entity
     * @return 响应DTO
     */
    public static CommentResponseDTO toResponseDTO(Comment comment) {
        if (comment == null) {
            return null;
        }
        
        CommentResponseDTO dto = new CommentResponseDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setArticleId(comment.getArticleId());
        dto.setUserId(comment.getUserId());
        dto.setParentId(comment.getParentId());
        dto.setToUserId(comment.getToUserId());
        dto.setStatus(comment.getStatus());
        dto.setCreateTime(comment.getCreateTime());
        dto.setIsReply(comment.getParentId() != null && comment.getParentId() > 0);
        
        return dto;
    }
    
    /**
     * Entity列表 转换为响应DTO列表
     * @param comments Entity列表
     * @return 响应DTO列表
     */
    public static List<CommentResponseDTO> toResponseDTOList(List<Comment> comments) {
        if (comments == null || comments.isEmpty()) {
            return new ArrayList<>();
        }
        
        List<CommentResponseDTO> dtos = new ArrayList<>();
        for (Comment comment : comments) {
            CommentResponseDTO dto = toResponseDTO(comment);
            if (dto != null) {
                dtos.add(dto);
            }
        }
        
        return dtos;
    }
}
