package dev.mintychochip.buildtools.api;

import java.util.Objects;
import java.util.UUID;

public record ActorId(UUID value) {
    public ActorId {
        Objects.requireNonNull(value, "value");
    }
}
