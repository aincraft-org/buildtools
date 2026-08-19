package dev.mintychochip.buildtools.api.command;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;
import java.util.Objects;

public record CommandContext(
        ActorId actorId,
        String worldId,
        BlockPosition origin,
        BlockPosition target,
        List<String> arguments) {
    public CommandContext {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(arguments, "arguments");
        arguments = List.copyOf(arguments);
    }

    public String argument(int index) {
        return index >= 0 && index < arguments.size() ? arguments.get(index) : null;
    }

    public List<String> rest(int fromIndex) {
        if (fromIndex >= arguments.size()) {
            return List.of();
        }
        return arguments.subList(fromIndex, arguments.size());
    }
}
