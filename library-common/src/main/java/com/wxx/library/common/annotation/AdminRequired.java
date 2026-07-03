package com.wxx.library.common.annotation;

import java.lang.annotation.*;

/**
 * 管理员权限注解（标注在方法上，仅管理员可访问）
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface AdminRequired {
}
