package dev.mintychochip.masonry.paper.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperPreviewRendererTest {
    @Test
    void packetDisplayTrackerReplacesAndClearsClientIds() {
        PaperPreviewRenderer.PacketDisplayTracker<String> tracker = new PaperPreviewRenderer.PacketDisplayTracker<>();
        BlockPosition first = new BlockPosition("world", 1, 64, 1);
        BlockPosition second = new BlockPosition("world", 2, 64, 1);
        BlockPosition third = new BlockPosition("world", 3, 64, 1);

        tracker.replace(Map.of(first, "id-1", second, "id-2"));
        assertEquals(Map.of(first, "id-1", second, "id-2"), tracker.entries());

        tracker.replace(Map.of(second, "id-3", third, "id-4"));
        assertEquals(Map.of(second, "id-3", third, "id-4"), tracker.entries());

        List<String> cleared = tracker.clear();
        assertEquals(2, cleared.size());
        assertTrue(cleared.contains("id-3"));
        assertTrue(cleared.contains("id-4"));
        assertTrue(tracker.entries().isEmpty());
    }
}
