package com.wreckloud.wolfchat.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Description OpenAPI 配置，配置 API 文档基本信息，确保 /api/v3/api-docs 可访问。Apifox 导入时使用: http://localhost:8080/api/v3/api-docs
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("WolfChat API 文档")
                        .description("WolfChat 聊天系统 API 接口文档")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Wreckloud"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")));
    }
}


