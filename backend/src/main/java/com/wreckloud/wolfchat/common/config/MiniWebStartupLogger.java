package com.wreckloud.wolfchat.common.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.InetAddress;

/**
 * @Description Web 版小程序入口启动日志
 * @Author Wreckloud
 * @Date 2026-03-27
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MiniWebStartupLogger {
    private static final String MINI_WEB_SUFFIX = "/mini-web/";

    private final Environment environment;

    @EventListener(ApplicationReadyEvent.class)
    public void logMiniWebEntry() {
        int port = resolvePort();
        String contextPath = normalizeContextPath(environment.getProperty("server.servlet.context-path"));
        String miniWebPath = contextPath + MINI_WEB_SUFFIX;
        String localhostUrl = "http://localhost:" + port + miniWebPath;

        if (!isMiniWebResourceReady()) {
            log.warn("Web 版小程序静态资源未就绪，请先执行: cd front/webapp && npm run build:embed");
        }

        log.info("Web 版小程序地址: {}", localhostUrl);

        String localIp = resolveLocalIp();
        if (StringUtils.hasText(localIp)) {
            log.info("Web 版小程序地址(局域网): http://{}:{}{}", localIp, port, miniWebPath);
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

    private boolean isMiniWebResourceReady() {
        ClassPathResource resource = new ClassPathResource("static/mini-web/index.html");
        return resource.exists();
    }
}

