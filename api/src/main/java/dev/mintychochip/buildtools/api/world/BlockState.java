package dev.mintychochip.buildtools.api.world;

import java.util.Map;
import java.util.Objects;

public record BlockState(String namespacedKey, Map<String, String> properties) {
    public static final BlockState AIR = of("minecraft:air");

    public BlockState {
        Objects.requireNonNull(namespacedKey, "namespacedKey");
        if (namespacedKey.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        properties = Map.copyOf(properties == null ? Map.of() : properties);
    }

    public static BlockState of(String namespacedKey) {
        return new BlockState(namespacedKey, Map.of());
    }

    public boolean isAir() {
        return namespacedKey.equals("minecraft:air") || namespacedKey.equals("minecraft:cave_air")
                || namespacedKey.equals("minecraft:void_air");
    }

    public String itemKey() {
        return namespacedKey;
    }
}
