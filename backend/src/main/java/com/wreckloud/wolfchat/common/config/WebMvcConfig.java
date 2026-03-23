package com.wreckloud.wolfchat.common.config;

import com.wreckloud.wolfchat.common.security.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description Web MVC 配置，注册拦截器并配置放行路径
 * @Author Wreckloud
 * @Date 2026-01-06
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {
    private final JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 约定：匹配路径基于应用内路径（不包含 server.servlet.context-path）。
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        // 认证接口放行
                        "/auth/**",
                        "/admin/auth/login",
                        // 管理端静态页面放行
                        "/admin-console/**",
                        // 媒体上传/读取放行（通过上传令牌与签名链接控制）
                        "/media/upload",
                        "/media/object",
                        // OpenAPI 文档放行
                        "/v3/api-docs/**",
                        "/doc.html/**",
                        "/swagger-ui/**",
                        "/swagger-resources/**",
                        "/webjars/**"
                );
    }
}


