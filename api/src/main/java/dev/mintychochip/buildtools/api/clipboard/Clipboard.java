package dev.mintychochip.buildtools.api.clipboard;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * A volume of block states keyed by offset from the copy origin.
 *
 * <p>Air cells are retained so paste can clear holes.
 *
 * @param originWorldId world the copy was taken from
 * @param blocks immutable offset-to-state map, including air
 */
public record Clipboard(String originWorldId, Map<BlockOffset, BlockState> blocks) {
    /**
     * @throws NullPointerException if a component is {@code null}
     */
    public Clipboard {
        Objects.requireNonNull(originWorldId, "originWorldId");
        Objects.requireNonNull(blocks, "blocks");
        blocks = Map.copyOf(blocks);
    }

    /**
     * @param originWorldId world id
     * @return clipboard with no blocks
     */
    public static Clipboard empty(String originWorldId) {
        return new Clipboard(originWorldId, Map.of());
    }

    /** @return number of stored cells (including air) */
    public int size() {
        return blocks.size();
    }

    /** @return {@code true} if no cells are stored */
    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    /**
     * Maps each offset onto an absolute position at {@code origin}.
     *
     * @param origin paste origin (typically pos1)
     * @return immutable absolute placements
     */
    public Map<BlockPosition, BlockState> placedAt(BlockPosition origin) {
        Objects.requireNonNull(origin, "origin");
        Map<BlockPosition, BlockState> placed = new LinkedHashMap<>();
        blocks.forEach((offset, state) ->
                placed.put(origin.offset(offset.x(), offset.y(), offset.z()), state));
        return Map.copyOf(placed);
    }
}
