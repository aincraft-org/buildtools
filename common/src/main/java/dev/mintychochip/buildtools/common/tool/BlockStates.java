package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class BlockStates {
    private BlockStates() {}

    public static BlockState parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        String normalized = raw.contains(":") ? raw : "minecraft:" + raw;
        int bracket = normalized.indexOf('[');
        if (bracket < 0) {
            return BlockState.of(normalized);
        }
        if (!normalized.endsWith("]")) {
            throw new IllegalArgumentException("Malformed block state: " + raw);
        }
        String key = normalized.substring(0, bracket);
        String body = normalized.substring(bracket + 1, normalized.length() - 1);
        Map<String, String> properties = new LinkedHashMap<>();
        if (!body.isBlank()) {
            for (String part : body.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) {
                    throw new IllegalArgumentException("Malformed block state property: " + part);
                }
                properties.put(kv[0].trim(), kv[1].trim());
            }
        }
        return new BlockState(key, properties);
    }

    public static boolean matches(BlockState actual, BlockState pattern) {
        Objects.requireNonNull(actual, "actual");
        Objects.requireNonNull(pattern, "pattern");
        if (!actual.namespacedKey().equals(pattern.namespacedKey())) {
            return false;
        }
        if (pattern.properties().isEmpty()) {
            return true;
        }
        return actual.properties().entrySet().containsAll(pattern.properties().entrySet());
    }
}
