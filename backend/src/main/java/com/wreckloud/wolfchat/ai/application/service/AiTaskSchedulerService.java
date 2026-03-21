package com.wreckloud.wolfchat.ai.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.annotation.PreDestroy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * AI 延迟任务调度器。
 */
@Slf4j
@Service
public class AiTaskSchedulerService {
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2, new AiThreadFactory());
    private final Map<String, ScheduledFuture<?>> pendingTaskMap = new ConcurrentHashMap<>();

    public boolean schedule(String dedupKey, Integer minDelaySeconds, Integer maxDelaySeconds, Runnable task) {
        if (task == null) {
            return false;
        }
        String normalizedKey = normalizeKey(dedupKey);
        if (normalizedKey != null && pendingTaskMap.containsKey(normalizedKey)) {
            return false;
        }
        long delaySeconds = resolveDelaySeconds(minDelaySeconds, maxDelaySeconds);
        Runnable wrapped = () -> {
            try {
                task.run();
            } catch (Exception ex) {
                log.warn("AI 延迟任务执行失败: key={}, message={}", normalizedKey, ex.getMessage());
            } finally {
                if (normalizedKey != null) {
                    pendingTaskMap.remove(normalizedKey);
                }
            }
        };
        ScheduledFuture<?> future = scheduler.schedule(wrapped, delaySeconds, TimeUnit.SECONDS);
        if (normalizedKey != null) {
            pendingTaskMap.put(normalizedKey, future);
        }
        return true;
    }

    public boolean isPending(String dedupKey) {
        String normalizedKey = normalizeKey(dedupKey);
        if (normalizedKey == null) {
            return false;
        }
        return pendingTaskMap.containsKey(normalizedKey);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
        pendingTaskMap.clear();
    }

    private String normalizeKey(String dedupKey) {
        if (!StringUtils.hasText(dedupKey)) {
            return null;
        }
        return dedupKey.trim();
    }

    private long resolveDelaySeconds(Integer minDelaySeconds, Integer maxDelaySeconds) {
        int min = minDelaySeconds == null || minDelaySeconds < 0 ? 2 : minDelaySeconds;
        int max = maxDelaySeconds == null || maxDelaySeconds < min ? min : maxDelaySeconds;
        if (min == max) {
            return min;
        }
        return min + (long) (Math.random() * (max - min + 1));
    }

    private static class AiThreadFactory implements ThreadFactory {
        private final AtomicInteger threadIndex = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("wolfchat-ai-" + threadIndex.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    }
}
