package com.wreckloud.wolfchat.ai.application.support;

import com.wreckloud.wolfchat.ai.config.AiConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

/**
 * AI 时段节奏支持：按时间窗口调整触发概率、冷却与延迟。
 */
@Component
@RequiredArgsConstructor
public class AiTimeRhythmSupport {
    private static final int DEFAULT_ACTIVE_START_HOUR = 9;
    private static final int DEFAULT_ACTIVE_END_HOUR = 23;
    private static final int DEFAULT_NIGHT_START_HOUR = 0;
    private static final int DEFAULT_NIGHT_END_HOUR = 6;
    private static final double DEFAULT_ACTIVE_PROBABILITY_MULTIPLIER = 1.12D;
    private static final double DEFAULT_NIGHT_PROBABILITY_MULTIPLIER = 0.65D;
    private static final double DEFAULT_ACTIVE_COOLDOWN_MULTIPLIER = 0.85D;
    private static final double DEFAULT_NIGHT_COOLDOWN_MULTIPLIER = 1.35D;
    private static final double DEFAULT_ACTIVE_DELAY_MULTIPLIER = 0.85D;
    private static final double DEFAULT_NIGHT_DELAY_MULTIPLIER = 1.45D;

    private final AiConfig aiConfig;

    public double adjustProbability(double probability) {
        if (probability <= 0D) {
            return 0D;
        }
        TimePeriod period = resolveTimePeriod();
        if (TimePeriod.ACTIVE.equals(period)) {
            return clampProbability(probability * resolveActiveProbabilityMultiplier());
        }
        if (TimePeriod.NIGHT.equals(period)) {
            return clampProbability(probability * resolveNightProbabilityMultiplier());
        }
        return clampProbability(probability);
    }

    public int adjustCooldown(Integer configuredCooldownSeconds, int fallbackSeconds) {
        int base = configuredCooldownSeconds == null || configuredCooldownSeconds <= 0
                ? fallbackSeconds
                : configuredCooldownSeconds;
        TimePeriod period = resolveTimePeriod();
        if (TimePeriod.ACTIVE.equals(period)) {
            return clampPositive((int) Math.round(base * resolveActiveCooldownMultiplier()), 4, 600);
        }
        if (TimePeriod.NIGHT.equals(period)) {
            return clampPositive((int) Math.round(base * resolveNightCooldownMultiplier()), 4, 600);
        }
        return clampPositive(base, 4, 600);
    }

    public DelayWindow adjustDelayWindow(int minDelaySeconds, int maxDelaySeconds) {
        int min = Math.max(1, minDelaySeconds);
        int max = Math.max(min, maxDelaySeconds);
        TimePeriod period = resolveTimePeriod();
        double multiplier = 1D;
        if (TimePeriod.ACTIVE.equals(period)) {
            multiplier = resolveActiveDelayMultiplier();
        } else if (TimePeriod.NIGHT.equals(period)) {
            multiplier = resolveNightDelayMultiplier();
        }
        int adjustedMin = clampPositive((int) Math.round(min * multiplier), 1, 240);
        int adjustedMax = clampPositive((int) Math.round(max * multiplier), adjustedMin, 300);
        return new DelayWindow(adjustedMin, adjustedMax);
    }

    public String currentPeriodLabel() {
        TimePeriod period = resolveTimePeriod();
        return switch (period) {
            case ACTIVE -> "active";
            case NIGHT -> "night";
            default -> "normal";
        };
    }

    private TimePeriod resolveTimePeriod() {
        AiConfig.Rhythm rhythm = aiConfig.getRhythm();
        if (rhythm == null || !Boolean.TRUE.equals(rhythm.getEnabled())) {
            return TimePeriod.NORMAL;
        }
        int hour = LocalTime.now().getHour();
        if (inHourRange(hour, resolveActiveStartHour(), resolveActiveEndHour())) {
            return TimePeriod.ACTIVE;
        }
        if (inHourRange(hour, resolveNightStartHour(), resolveNightEndHour())) {
            return TimePeriod.NIGHT;
        }
        return TimePeriod.NORMAL;
    }

    private boolean inHourRange(int hour, int startInclusive, int endExclusive) {
        if (startInclusive == endExclusive) {
            return false;
        }
        if (startInclusive < endExclusive) {
            return hour >= startInclusive && hour < endExclusive;
        }
        return hour >= startInclusive || hour < endExclusive;
    }

    private int resolveActiveStartHour() {
        return clampHour(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getActiveStartHour(), DEFAULT_ACTIVE_START_HOUR);
    }

    private int resolveActiveEndHour() {
        return clampHour(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getActiveEndHour(), DEFAULT_ACTIVE_END_HOUR);
    }

    private int resolveNightStartHour() {
        return clampHour(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getNightStartHour(), DEFAULT_NIGHT_START_HOUR);
    }

    private int resolveNightEndHour() {
        return clampHour(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getNightEndHour(), DEFAULT_NIGHT_END_HOUR);
    }

    private int clampHour(Integer configured, int fallback) {
        if (configured == null || configured < 0 || configured > 23) {
            return fallback;
        }
        return configured;
    }

    private double resolveActiveProbabilityMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getActiveProbabilityMultiplier(),
                DEFAULT_ACTIVE_PROBABILITY_MULTIPLIER);
    }

    private double resolveNightProbabilityMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getNightProbabilityMultiplier(),
                DEFAULT_NIGHT_PROBABILITY_MULTIPLIER);
    }

    private double resolveActiveCooldownMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getActiveCooldownMultiplier(),
                DEFAULT_ACTIVE_COOLDOWN_MULTIPLIER);
    }

    private double resolveNightCooldownMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getNightCooldownMultiplier(),
                DEFAULT_NIGHT_COOLDOWN_MULTIPLIER);
    }

    private double resolveActiveDelayMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getActiveDelayMultiplier(),
                DEFAULT_ACTIVE_DELAY_MULTIPLIER);
    }

    private double resolveNightDelayMultiplier() {
        return resolveMultiplier(aiConfig.getRhythm() == null ? null : aiConfig.getRhythm().getNightDelayMultiplier(),
                DEFAULT_NIGHT_DELAY_MULTIPLIER);
    }

    private double resolveMultiplier(Double configured, double fallback) {
        if (configured == null || configured <= 0D) {
            return fallback;
        }
        return Math.max(0.2D, Math.min(configured, 3.0D));
    }

    private int clampPositive(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    private double clampProbability(double value) {
        if (value <= 0D) {
            return 0D;
        }
        if (value >= 1D) {
            return 1D;
        }
        return value;
    }

    public record DelayWindow(int minDelaySeconds, int maxDelaySeconds) {
    }

    private enum TimePeriod {
        ACTIVE,
        NIGHT,
        NORMAL
    }
}
