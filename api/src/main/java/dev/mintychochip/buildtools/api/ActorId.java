package dev.mintychochip.buildtools.api;

import java.util.Objects;
import java.util.UUID;

/**
 * Stable identifier for a player (or other actor) that owns selections, history, and blueprints.
 *
 * @param value non-null player UUID
 */
public record ActorId(UUID value) {
    /**
     * @param value player UUID
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public ActorId {
        Objects.requireNonNull(value, "value");
    }
}
