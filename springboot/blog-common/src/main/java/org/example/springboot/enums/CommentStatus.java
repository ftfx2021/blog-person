package org.example.springboot.enums;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 评论状态枚举
 */
public enum CommentStatus {
    @Schema(description = "待审核")
    PENDING(0, "待审核"),
    
    @Schema(description = "已通过")
    APPROVED(1, "已通过"),
    
    @Schema(description = "已拒绝")
    REJECTED(2, "已拒绝");

    private final Integer value;
    private final String description;

    CommentStatus(Integer value, String description) {
        this.value = value;
        this.description = description;
    }

    public Integer getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * 根据值获取枚举
     * @param value 状态值
     * @return 枚举
     */
    public static CommentStatus fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (CommentStatus status : values()) {
            if (status.getValue().equals(value)) {
                return status;
            }
        }
        throw new IllegalArgumentException("无效的评论状态值: " + value);
    }

    /**
     * 验证状态流转是否有效
     * @param fromStatus 原状态
     * @param toStatus 目标状态
     * @return 是否有效
     */
    public static boolean isValidTransition(CommentStatus fromStatus, CommentStatus toStatus) {
        if (fromStatus == null || toStatus == null) {
            return false;
        }
        
        // 待审核可以转为任何状态
        if (fromStatus == PENDING) {
            return true;
        }
        
        // 已通过和已拒绝状态不能再次变更
        return false;
    }
}
