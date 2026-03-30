package com.wreckloud.wolfchat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Description Spring Boot 应用入口
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class WolfChatApplication {

    public static void main(String[] args) {
        SpringApplication.run(WolfChatApplication.class, args);
    }
}

