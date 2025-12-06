package com.wreckloud.wolfchat.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description Knife4j API文档配置
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Configuration
public class Knife4jConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WolfChat API 文档")
                        .version("1.0.0")
                        .description("WolfChat 聊天系统 API 接口文档")
                        .contact(new Contact()
                                .name("Wreckloud")
                                .email("your-email@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}

