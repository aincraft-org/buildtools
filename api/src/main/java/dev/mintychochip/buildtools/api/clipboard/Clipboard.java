package dev.mintychochip.buildtools.api.clipboard;

import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.api.world.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record Clipboard(String originWorldId, Map<BlockOffset, BlockState> blocks) {
    public Clipboard {
        Objects.requireNonNull(originWorldId, "originWorldId");
        Objects.requireNonNull(blocks, "blocks");
        blocks = Map.copyOf(blocks);
    }

    public static Clipboard empty(String originWorldId) {
        return new Clipboard(originWorldId, Map.of());
    }

    public int size() {
        return blocks.size();
    }

    public boolean isEmpty() {
        return blocks.isEmpty();
    }

    public Map<BlockPosition, BlockState> placedAt(BlockPosition origin) {
        Objects.requireNonNull(origin, "origin");
        Map<BlockPosition, BlockState> placed = new LinkedHashMap<>();
        blocks.forEach((offset, state) ->
                placed.put(origin.offset(offset.x(), offset.y(), offset.z()), state));
        return Map.copyOf(placed);
    }
}
