package dev.mintychochip.buildtools.api.blueprint;

import dev.mintychochip.buildtools.api.ActorId;
import java.time.Instant;
import java.util.Objects;

/**
 * Listing metadata for one saved blueprint. The block body is stored separately.
 *
 * @param name owner-unique name
 * @param owner owning player
 * @param createdAt save time
 * @param width inclusive width
 * @param height inclusive height
 * @param length inclusive length (Z)
 */
public record BlueprintMeta(String name, ActorId owner, Instant createdAt, int width, int height, int length) {
    /**
     * @throws NullPointerException if name, owner, or createdAt is {@code null}
     * @throws IllegalArgumentException if name is blank or a dimension is not positive
     */
    public BlueprintMeta {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(createdAt, "createdAt");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must be present");
        }
        if (width <= 0 || height <= 0 || length <= 0) {
            throw new IllegalArgumentException("dimensions must be positive");
        }
    }
}
