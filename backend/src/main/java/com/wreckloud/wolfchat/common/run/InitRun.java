package com.wreckloud.wolfchat.common.run;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * @Description 启动提示 Runner，检测核心依赖并打印启动结果
 * @Author Wreckloud
 * @Date 2025-12-08
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InitRun implements ApplicationRunner {

    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate jdbcTemplate;
    private final Environment env;

    @Override
    public void run(ApplicationArguments args) {
        boolean redisOk = checkRedis();
        boolean dbOk = checkDb();
        String port = env.getProperty("server.port", "8080");
        String context = env.getProperty("server.servlet.context-path", "");

        if (redisOk && dbOk) {
            log.info("WolfChat 启动成功，接口地址：http://localhost:{}{}/doc.html", port, context);
        } else {
            log.warn("WolfChat 启动完成，但依赖检查存在问题：redisOk={}, dbOk={}", redisOk, dbOk);
        }
    }

    private boolean checkRedis() {
        try {
            String pong = redisTemplate.getConnectionFactory().getConnection().ping();
            return "PONG".equalsIgnoreCase(pong);
        } catch (Exception e) {
            log.error("Redis 连接检查失败: {}", e.getMessage());
            return false;
        }
    }

    private boolean checkDb() {
        try {
            Integer one = jdbcTemplate.queryForObject("select 1", Integer.class);
            return one != null && one == 1;
        } catch (Exception e) {
            log.error("数据库连接检查失败: {}", e.getMessage());
            return false;
        }
    }
}

