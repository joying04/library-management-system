package com.wxx.library;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 图书借阅系统启动类
 */
@SpringBootApplication
@MapperScan("com.wxx.library.mapper")
@EnableScheduling // 启用Spring定时任务
@EnableRetry //开启Spring重试机制
public class LibraryApplication {

    public static void main(String[] args) {
        SpringApplication.run(LibraryApplication.class, args);
        System.out.println("图书借阅系统启动成功！访问地址：http://localhost:8080/api/swagger-ui/index.html");
    }
}
