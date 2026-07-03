package com.wxx.library.common.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;

@Data
@TableName("user")
@Schema(name = "User对象", description = "用户表")
public class User {

    @Schema(description = "用户ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @Schema(description = "手机号（登录账号）", required = true)
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Schema(description = "密码（BCrypt加密）", required = true)
    @NotBlank(message = "密码不能为空")
    @Length(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @JsonIgnore
    private String password;

    @Schema(description = "用户姓名", required = true)
    @NotBlank(message = "姓名不能为空")
    @TableField("username")
    private String name;

    @Schema(description = "角色：0-普通用户，1-管理员", example = "0")
    private Integer role;

    @Schema(description = "状态：0-禁用，1-正常", example = "1")
    private Integer status;

    @Schema(description = "最大借阅数量", example = "5")
    @TableField("max_borrow_count")
    private Integer maxBorrowCount;

    @Schema(description = "创建时间")
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @Schema(description = "逻辑删除：0-未删除，1-已删除", example = "0")
    @TableLogic
    private Integer deleted;

    @Schema(description = "乐观锁版本号", example = "1")
    @Version
    private Integer version;
}
