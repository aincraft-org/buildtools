package dev.mintychochip.masonry.api.command;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One {@code /masonry} invocation after the Paper adapter has resolved origin and look-target.
 *
 * @param actorId invoking player
 * @param worldId current world name
 * @param origin player feet block (used as interaction origin)
 * @param target looked-at block, or {@code null} if none
 * @param arguments raw tokens including the subcommand
 * @param excludedPositions cells the operation must never write (the actor's body cells)
 */
public record CommandContext(
        ActorId actorId,
        String worldId,
        BlockPosition origin,
        BlockPosition target,
        List<String> arguments,
        Set<BlockPosition> excludedPositions) {
    /**
     * @throws NullPointerException if actor, world, origin, or arguments is {@code null}
     */
    public CommandContext {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(worldId, "worldId");
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(arguments, "arguments");
        arguments = List.copyOf(arguments);
        excludedPositions = excludedPositions == null
                ? Set.of()
                : Set.copyOf(excludedPositions);
    }

    /**
     * Context without explicit exclusions.
     *
     * @param actorId actor
     * @param worldId world
     * @param origin origin
     * @param target target
     * @param arguments arguments
     */
    public CommandContext(
            ActorId actorId,
            String worldId,
            BlockPosition origin,
            BlockPosition target,
            List<String> arguments) {
        this(actorId, worldId, origin, target, arguments, Set.of());
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
