package com.wreckloud.wolfchat.common.config;

import com.google.code.kaptcha.impl.DefaultKaptcha;
import com.google.code.kaptcha.util.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Properties;

/**
 * @Description 验证码配置类
 * @Author Wreckloud
 * @Date 2025-12-06
 */
@Configuration
public class KaptchaConfig {

    @Bean
    public DefaultKaptcha defaultKaptcha() {
        DefaultKaptcha kaptcha = new DefaultKaptcha();
        Properties properties = new Properties();
        
        // 图片边框
        properties.setProperty("kaptcha.border", "yes");
        properties.setProperty("kaptcha.border.color", "105,179,90");
        
        // 字体颜色
        properties.setProperty("kaptcha.textproducer.font.color", "blue");
        
        // 图片宽高
        properties.setProperty("kaptcha.image.width", "120");
        properties.setProperty("kaptcha.image.height", "40");
        
        // 字体大小
        properties.setProperty("kaptcha.textproducer.font.size", "30");
        
        // 验证码字符集
        properties.setProperty("kaptcha.textproducer.char.string", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
        
        // 字符个数
        properties.setProperty("kaptcha.textproducer.char.length", "4");
        
        // 字体
        properties.setProperty("kaptcha.textproducer.font.names", "Arial,Courier");
        
        // 字符间距
        properties.setProperty("kaptcha.textproducer.char.space", "3");
        
        // 干扰线颜色
        properties.setProperty("kaptcha.noise.color", "white");
        
        Config config = new Config(properties);
        kaptcha.setConfig(config);
        return kaptcha;
    }
}


