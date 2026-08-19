package dev.mintychochip.buildtools.api.blueprint;

import dev.mintychochip.buildtools.api.ActorId;
import java.time.Instant;
import java.util.Objects;

public record BlueprintMeta(String name, ActorId owner, Instant createdAt, int width, int height, int length) {
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
