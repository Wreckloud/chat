package com.wreckloud.wolfchat.common.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * @Description mybatisPlus的配置
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Configuration
@MapperScan({
        "com.wreckloud.wolfchat.account.infra.mapper"
})
public class MybatisPlusConfig {
}
