package dev.mintychochip.buildtools.api.command;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.List;
import java.util.Objects;

/**
 * One {@code /bt} invocation after the Paper adapter has resolved origin and look-target.
 *
 * @param actorId invoking player
 * @param worldId current world name
 * @param origin player feet block (used as interaction origin)
 * @param target looked-at block, or {@code null} if none
 * @param arguments raw tokens including the subcommand
 */
public record CommandContext(
        ActorId actorId,
        String worldId,
        BlockPosition origin,
        BlockPosition target,
        List<String> arguments) {
    /**
     * @throws NullPointerException if actor, world, origin, or arguments is {@code null}
     */
    public CommandContext {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(arguments, "arguments");
        arguments = List.copyOf(arguments);
    }

    /**
     * @param index argument index
     * @return token at {@code index}, or {@code null} if out of range
     */
    public String argument(int index) {
        return index >= 0 && index < arguments.size() ? arguments.get(index) : null;
    }

    /**
     * @param fromIndex inclusive start
     * @return remaining tokens, possibly empty
     */
    public List<String> rest(int fromIndex) {
        if (fromIndex >= arguments.size()) {
            return List.of();
        }
        return arguments.subList(fromIndex, arguments.size());
    }
}
