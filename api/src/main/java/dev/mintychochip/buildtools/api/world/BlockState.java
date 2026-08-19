package dev.mintychochip.buildtools.api.world;

import java.util.Map;
import java.util.Objects;

/**
 * A namespaced block type plus optional block-state properties (for example facing, half).
 *
 * @param namespacedKey block id such as {@code minecraft:oak_stairs}
 * @param properties immutable property map; empty for default state
 */
public record BlockState(String namespacedKey, Map<String, String> properties) {
    /** Default air. */
    public static final BlockState AIR = of("minecraft:air");

    /**
     * @throws NullPointerException if {@code namespacedKey} is {@code null}
     * @throws IllegalArgumentException if {@code namespacedKey} is blank
     */
    public BlockState {
        Objects.requireNonNull(namespacedKey, "namespacedKey");
        if (namespacedKey.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        properties = Map.copyOf(properties == null ? Map.of() : properties);
    }

    /**
     * Creates a default-state block with no properties.
     *
     * @param namespacedKey block id
     * @return new state
     */
    public static BlockState of(String namespacedKey) {
        return new BlockState(namespacedKey, Map.of());
    }

    /**
     * @return {@code true} for air, cave air, or void air
     */
    public boolean isAir() {
        return namespacedKey.equals("minecraft:air") || namespacedKey.equals("minecraft:cave_air")
                || namespacedKey.equals("minecraft:void_air");
    }

    /**
     * Item id used when charging or refunding this block. V1 uses the block namespaced key.
     *
     * @return inventory item key
     */
    public String itemKey() {
        return namespacedKey;
    }
}
