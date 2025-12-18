package com.wreckloud.wolfchat.common.security.config;

import com.wreckloud.wolfchat.common.security.interceptor.AdminInterceptor;
import com.wreckloud.wolfchat.common.security.interceptor.JwtInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Description Web配置类，配置拦截器
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final AdminInterceptor adminInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT认证拦截器（优先级1）
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有需要登录的接口
                .addPathPatterns(
                        "/group/**",
                        "/message/**",
                        "/friend/**",
                        "/conversation/**",
                        "/admin/**"  // 管理员接口也需要先登录
                )
                // 排除不需要登录的接口
                .excludePathPatterns(
                        "/account/login/**",
                        "/account/register/**",
                        "/account/captcha",
                        "/account/sms/send",
                        "/account/user/**",
                        "/doc.html",
                        "/swagger-resources/**",
                        "/v3/api-docs/**",
                        "/webjars/**",
                        "/favicon.ico"
                )
                .order(1);
        
        // 管理员权限拦截器（优先级2，在JWT之后）
        registry.addInterceptor(adminInterceptor)
                .addPathPatterns("/admin/**")
                .order(2);
    }
}

