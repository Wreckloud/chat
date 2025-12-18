package com.wreckloud.wolfchat.common.security.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Description 需要登录验证的注解
 *
 * @author Wreckloud
 * @date 2024-12-18
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireLogin {
    /**
     * 是否必须登录，默认为true
     */
    boolean required() default true;
}

