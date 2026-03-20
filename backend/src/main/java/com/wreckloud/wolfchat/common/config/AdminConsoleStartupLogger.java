package com.wreckloud.wolfchat.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.core.io.ClassPathResource;

import java.net.InetAddress;

/**
 * @Description 管理端入口启动日志
 * @Author Wreckloud
 * @Date 2026-03-20
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminConsoleStartupLogger {
    private static final String ADMIN_CONSOLE_SUFFIX = "/admin-console/";

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logAdminConsoleEntry() {
        int port = resolvePort();
        String contextPath = normalizeContextPath(environment.getProperty("server.servlet.context-path"));
        String adminPath = contextPath + ADMIN_CONSOLE_SUFFIX;
        String localhostUrl = "http://localhost:" + port + adminPath;

        if (!isAdminConsoleResourceReady()) {
            log.warn("管理端静态资源未就绪，请先执行: mvn -DskipTests package 或 npm run build:embed");
        }

        log.info("管理端地址: {}", localhostUrl);

        String localIp = resolveLocalIp();
        if (StringUtils.hasText(localIp)) {
            log.info("管理端地址(局域网): http://{}:{}{}", localIp, port, adminPath);
        }
    }

    private int resolvePort() {
        Integer localServerPort = environment.getProperty("local.server.port", Integer.class);
        if (localServerPort != null) {
            return localServerPort;
        }
        Integer serverPort = environment.getProperty("server.port", Integer.class);
        if (serverPort != null) {
            return serverPort;
        }
        return 8080;
    }

    private String normalizeContextPath(String rawContextPath) {
        if (!StringUtils.hasText(rawContextPath)) {
            return "";
        }
        String contextPath = rawContextPath.trim();
        if ("/".equals(contextPath)) {
            return "";
        }
        if (!contextPath.startsWith("/")) {
            contextPath = "/" + contextPath;
        }
        if (contextPath.endsWith("/")) {
            contextPath = contextPath.substring(0, contextPath.length() - 1);
        }
        return contextPath;
    }

    private String resolveLocalIp() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            log.debug("读取本机IP失败", e);
            return null;
        }
    }

    private boolean isAdminConsoleResourceReady() {
        ClassPathResource resource = new ClassPathResource("static/admin-console/index.html");
        return resource.exists();
    }
}
