package com.wxx.library.borrow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wxx.library.borrow.mapper")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.wxx.library.common.feign")
@EnableScheduling
public class BorrowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BorrowApplication.class, args);
        System.out.println("借阅服务启动成功！");
    }
}
