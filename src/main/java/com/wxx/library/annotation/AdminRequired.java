package com.wxx.library.annotation;


import java.lang.annotation.*;

/**
 * 管理员权限注解（标注在方法上，仅管理员可访问）
 */
@Target({ElementType.METHOD}) // 仅作用于方法
@Retention(RetentionPolicy.RUNTIME) // 运行时生效
@Documented
public @interface AdminRequired {
    // 无参数，仅作为标记
}
