package com.metal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/** 金属厂数据管理系统 - Spring Boot 启动入口 */
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class MetalApplication {
    public static void main(String[] args) {
        SpringApplication.run(MetalApplication.class, args);
    }
}
