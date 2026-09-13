package com.monibank.mainframe.operations;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class OperationEventJsonCodec {

    private static final Pattern FIELD = Pattern.compile(
            "\\\"([^\\\"]+)\\\"\\s*:\\s*"
                    + "(?:\\\"((?:\\\\.|[^\\\"])*)\\\""
                    + "|(-?\\d+)"
                    + "|(true|false))"
    );

    private OperationEventJsonCodec() {
    }

    static String encode(CoreOperationEvent event) {
        StringBuilder json = new StringBuilder(512).append('{');
        append(json, "requestId", event.requestId());
        append(json, "core", event.core());
        append(json, "provider", event.provider());
        append(json, "systemId", event.systemId());
        append(json, "channel", event.channel());
        append(json, "operation", event.operation());
        append(json, "executorType", event.executorType());
        append(json, "executorId", event.executorId());
        append(json, "username", event.username());
        append(json, "acceptedAt", event.acceptedAt());
        append(json, "assignedAt", event.assignedAt());
        append(json, "completedAt", event.completedAt());
        append(json, "queueMs", event.queueMs());
        append(json, "durationMs", event.durationMs());
        append(json, "status", event.status());
        append(json, "resultCode", event.resultCode());
        append(json, "errorType", event.errorType());
        append(json, "retryable", event.retryable());
        return json.append('}').toString();
    }

    static CoreOperationEvent decode(String json) {
        Map<String, String> values = new HashMap<>();
        Matcher matcher = FIELD.matcher(json);

        while (matcher.find()) {
            String value;
            if (matcher.group(2) != null) {
                value = unescape(matcher.group(2));
            } else if (matcher.group(3) != null) {
                value = matcher.group(3);
            } else {
                value = matcher.group(4);
            }
            values.put(matcher.group(1), value);
        }

        return new CoreOperationEvent(
                required(values, "requestId"),
                CoreOperationEvent.Core.valueOf(required(values, "core")),
                required(values, "provider"),
                required(values, "systemId"),
                required(values, "channel"),
                required(values, "operation"),
                CoreOperationEvent.ExecutorType.valueOf(
                        required(values, "executorType")
                ),
                values.get("executorId"),
                values.get("username"),
                Instant.parse(required(values, "acceptedAt")),
                instant(values.get("assignedAt")),
                Instant.parse(required(values, "completedAt")),
                number(values, "queueMs"),
                number(values, "durationMs"),
                CoreOperationEvent.Status.valueOf(
                        required(values, "status")
                ),
                values.get("resultCode"),
                values.get("errorType"),
                bool(values.get("retryable"))
        );
    }

    private static String required(
            Map<String, String> values,
            String name
    ) {
        String value = values.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(
                    "Missing operation event field: " + name
            );
        }
        return value;
    }

    private static long number(
            Map<String, String> values,
            String name
    ) {
        return Long.parseLong(required(values, name));
    }

    private static Instant instant(String value) {
        return value == null ? null : Instant.parse(value);
    }

    private static Boolean bool(String value) {
        return value == null ? null : Boolean.valueOf(value);
    }

    private static void append(
            StringBuilder json,
            String name,
            Object value
    ) {
        if (value == null) {
            return;
        }
        if (json.length() > 1) {
            json.append(',');
        }
        quote(json, name).append(':');

        if (value instanceof Number || value instanceof Boolean) {
            json.append(value);
        } else {
            quote(json, value.toString());
        }
    }

    private static StringBuilder quote(
            StringBuilder json,
            String value
    ) {
        json.append('"');
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '"' -> json.append("\\\"");
                case '\\' -> json.append("\\\\");
                case '\b' -> json.append("\\b");
                case '\f' -> json.append("\\f");
                case '\n' -> json.append("\\n");
                case '\r' -> json.append("\\r");
                case '\t' -> json.append("\\t");
                default -> {
                    if (character < 0x20) {
                        json.append(String.format("\\u%04x", (int) character));
                    } else {
                        json.append(character);
                    }
                }
            }
        }
        return json.append('"');
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder(value.length());
        boolean escaped = false;

        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            if (!escaped && character == '\\') {
                escaped = true;
                continue;
            }
            if (!escaped) {
                result.append(character);
                continue;
            }

            switch (character) {
                case '"', '\\', '/' -> result.append(character);
                case 'b' -> result.append('\b');
                case 'f' -> result.append('\f');
                case 'n' -> result.append('\n');
                case 'r' -> result.append('\r');
                case 't' -> result.append('\t');
                case 'u' -> {
                    if (index + 4 >= value.length()) {
                        throw new IllegalArgumentException(
                                "Incomplete JSON unicode escape."
                        );
                    }
                    String hex = value.substring(index + 1, index + 5);
                    result.append((char) Integer.parseInt(hex, 16));
                    index += 4;
                }
                default -> throw new IllegalArgumentException(
                        "Unsupported JSON escape sequence."
                );
            }
            escaped = false;
        }

        if (escaped) {
            throw new IllegalArgumentException("Incomplete JSON escape.");
        }
        return result.toString();
    }
}
