package com.wxx.library.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    int maxCount();  //周期内最大请求数
    long period();  //周期(秒)
}
