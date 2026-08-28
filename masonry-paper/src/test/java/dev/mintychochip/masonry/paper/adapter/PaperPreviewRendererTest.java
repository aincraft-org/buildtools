package dev.mintychochip.masonry.paper.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PaperPreviewRendererTest {
    @Test
    void fakeBlockTrackerRestoresStaleAndClearedPositions() {
        PaperPreviewRenderer.FakeBlockTracker<String> tracker = new PaperPreviewRenderer.FakeBlockTracker<>();
        BlockPosition first = new BlockPosition("world", 1, 64, 1);
        BlockPosition second = new BlockPosition("world", 2, 64, 1);
        BlockPosition third = new BlockPosition("world", 3, 64, 1);
        Map<BlockPosition, String> sent = new HashMap<>();
        List<String> restored = new ArrayList<>();

        tracker.show(
                Map.of(first, "glass", second, "glass"),
                position -> "original-" + position.x(),
                sent::put,
                (position, original) -> restored.add(position.x() + ":" + original));

        assertEquals(Map.of(first, "glass", second, "glass"), sent);
        assertTrue(restored.isEmpty());
        assertFalse(tracker.isEmpty());

        sent.clear();
        tracker.show(
                Map.of(second, "tinted", third, "glass"),
                position -> "recaptured-" + position.x(),
                sent::put,
                (position, original) -> restored.add(position.x() + ":" + original));

        assertEquals(List.of("1:original-1"), restored);
        assertEquals(Map.of(second, "tinted", third, "glass"), sent);

        tracker.clear((position, original) -> restored.add(position.x() + ":" + original));

        assertEquals(3, restored.size());
        assertTrue(restored.contains("1:original-1"));
        assertTrue(restored.contains("2:original-2"));
        assertTrue(restored.contains("3:recaptured-3"));
        assertTrue(tracker.isEmpty());
    }

    @Test
    void fakeBlockTrackerResendsOnlyTrackedShownPositions() {
        PaperPreviewRenderer.FakeBlockTracker<String> tracker = new PaperPreviewRenderer.FakeBlockTracker<>();
        BlockPosition first = new BlockPosition("world", 1, 64, 1);
        BlockPosition second = new BlockPosition("world", 2, 64, 1);
        Map<BlockPosition, String> resent = new HashMap<>();

        tracker.show(
                Map.of(first, "glass", second, "tinted"),
                position -> "original-" + position.x(),
                (position, data) -> {},
                (position, original) -> {});
        tracker.resend(position -> position.x() == 2, resent::put);

        assertEquals(Map.of(second, "tinted"), resent);
    }
}
