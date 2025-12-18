package com.wreckloud.wolfchat.common.security.config;

import com.wreckloud.wolfchat.common.security.interceptor.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
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
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(jwtInterceptor)
                // 拦截所有需要登录的接口
                .addPathPatterns(
                        "/group/**",
                        "/message/**",
                        "/friend/**",
                        "/conversation/**"
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
                );
    }
}

