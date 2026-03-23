package com.wreckloud.wolfchat.ai.application.support;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * @Description AI 回复拆分支持
 * @Author Wreckloud
 * @Date 2026-03-23
 */
@Component
public class AiReplySplitSupport {
    private static final int PRIVATE_MAX_SEGMENTS = 4;
    private static final int PRIVATE_MIN_SEGMENTS = 2;
    private static final int PRIVATE_MAX_CHARS = 60;

    private static final int LOBBY_MAX_SEGMENTS = 4;
    private static final int LOBBY_MIN_SEGMENTS = 2;
    private static final int LOBBY_MAX_CHARS = 48;

    private static final int MIN_SEGMENT_CHARS = 8;

    public List<String> splitPrivateReply(String reply) {
        return splitForChat(reply, PRIVATE_MIN_SEGMENTS, PRIVATE_MAX_SEGMENTS, MIN_SEGMENT_CHARS, PRIVATE_MAX_CHARS);
    }

    public List<String> splitLobbyReply(String reply) {
        return splitForChat(reply, LOBBY_MIN_SEGMENTS, LOBBY_MAX_SEGMENTS, MIN_SEGMENT_CHARS, LOBBY_MAX_CHARS);
    }

    private List<String> splitForChat(String reply,
                                      int minSegments,
                                      int maxSegments,
                                      int minSegmentChars,
                                      int maxSegmentChars) {
        String normalized = normalizeText(reply);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        if (normalized.length() <= maxSegmentChars) {
            return List.of(normalized);
        }

        List<String> segments = splitByPrimaryPunctuation(normalized);
        segments = normalizeSegments(segments, maxSegmentChars);
        segments = mergeShortSegments(segments, minSegmentChars);

        if (segments.size() < minSegments) {
            segments = splitLargeSegments(segments, maxSegmentChars);
            segments = mergeShortSegments(segments, minSegmentChars);
        }
        if (segments.size() > maxSegments) {
            segments = mergeToMaxSegments(segments, maxSegments);
        }
        return segments.isEmpty() ? List.of(normalized) : segments;
    }

    private String normalizeText(String reply) {
        if (!StringUtils.hasText(reply)) {
            return null;
        }
        String normalized = reply.replace("\r", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.replaceAll("[\\t ]+", " ");
    }

    private List<String> splitByPrimaryPunctuation(String text) {
        List<String> parts = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            current.append(ch);
            if (isPrimaryBoundary(ch)) {
                appendPart(parts, current.toString());
                current.setLength(0);
            }
        }
        appendPart(parts, current.toString());
        return parts;
    }

    private boolean isPrimaryBoundary(char ch) {
        return ch == '。' || ch == '！' || ch == '？' || ch == '；'
                || ch == '!' || ch == '?' || ch == ';'
                || ch == '…' || ch == '\n';
    }

    private List<String> normalizeSegments(List<String> segments, int maxSegmentChars) {
        List<String> normalized = new ArrayList<>();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            if (segment.length() <= maxSegmentChars) {
                appendPart(normalized, segment);
                continue;
            }
            for (String chunk : splitLargeChunk(segment, maxSegmentChars)) {
                appendPart(normalized, chunk);
            }
        }
        return normalized;
    }

    private List<String> splitLargeChunk(String text, int maxSegmentChars) {
        List<String> result = new ArrayList<>();
        String remaining = text.trim();
        while (remaining.length() > maxSegmentChars) {
            int cut = findCutPoint(remaining, maxSegmentChars);
            appendPart(result, remaining.substring(0, cut));
            remaining = remaining.substring(cut).trim();
        }
        appendPart(result, remaining);
        return result;
    }

    private int findCutPoint(String text, int maxSegmentChars) {
        int cut = text.lastIndexOf('，', maxSegmentChars);
        if (cut >= maxSegmentChars / 2) {
            return cut + 1;
        }
        cut = text.lastIndexOf('、', maxSegmentChars);
        if (cut >= maxSegmentChars / 2) {
            return cut + 1;
        }
        cut = text.lastIndexOf(',', maxSegmentChars);
        if (cut >= maxSegmentChars / 2) {
            return cut + 1;
        }
        cut = text.lastIndexOf(' ', maxSegmentChars);
        if (cut >= maxSegmentChars / 2) {
            return cut + 1;
        }
        return maxSegmentChars;
    }

    private List<String> splitLargeSegments(List<String> segments, int maxSegmentChars) {
        List<String> result = new ArrayList<>();
        for (String segment : segments) {
            if (segment.length() <= maxSegmentChars) {
                appendPart(result, segment);
                continue;
            }
            for (String chunk : splitLargeChunk(segment, maxSegmentChars)) {
                appendPart(result, chunk);
            }
        }
        return result;
    }

    private List<String> mergeShortSegments(List<String> segments, int minSegmentChars) {
        if (segments.size() <= 1) {
            return segments;
        }
        List<String> merged = new ArrayList<>();
        for (String segment : segments) {
            if (!StringUtils.hasText(segment)) {
                continue;
            }
            if (segment.length() < minSegmentChars && !merged.isEmpty()) {
                String last = merged.remove(merged.size() - 1);
                merged.add(last + segment);
                continue;
            }
            merged.add(segment);
        }
        return merged;
    }

    private List<String> mergeToMaxSegments(List<String> segments, int maxSegments) {
        if (segments.size() <= maxSegments) {
            return segments;
        }
        List<String> result = new ArrayList<>(segments.subList(0, maxSegments - 1));
        StringBuilder tailBuilder = new StringBuilder();
        for (int i = maxSegments - 1; i < segments.size(); i++) {
            if (tailBuilder.length() > 0) {
                tailBuilder.append(' ');
            }
            tailBuilder.append(segments.get(i));
        }
        appendPart(result, tailBuilder.toString());
        return result;
    }

    private void appendPart(List<String> holder, String raw) {
        if (!StringUtils.hasText(raw)) {
            return;
        }
        String part = raw.trim();
        if (!StringUtils.hasText(part)) {
            return;
        }
        holder.add(part);
    }
}

