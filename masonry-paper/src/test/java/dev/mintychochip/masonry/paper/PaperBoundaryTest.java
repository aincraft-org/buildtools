package dev.mintychochip.masonry.paper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PermissionService;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.TaskScheduler;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.paper.adapter.PaperPermissionService;
import dev.mintychochip.masonry.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.masonry.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.masonry.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.masonry.paper.adapter.PaperWorldAccess;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

/**
 * Checks {@code plugin.yml} metadata and that adapters implement API ports on the Paper side.
 */
class PaperBoundaryTest {
    @Test
    void pluginMetadataPointsAtPaperEntryPoint() throws Exception {
        try (InputStream in = MasonryPlugin.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in);
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("main: dev.mintychochip.masonry.paper.MasonryPlugin"));
            assertTrue(yaml.contains("api-version: \"26.2\"")
                    || yaml.contains("api-version: '26.2'")
                    || yaml.contains("api-version: 26.2"));
            assertTrue(yaml.contains("name: Masonry"));
            assertTrue(yaml.contains("masonry:") && !yaml.contains("bt:"));
            assertTrue(yaml.contains("0.1.0"));
        }
    }

    @Test
    void pluginAndAdaptersStayOnThePaperSideOfTheBoundary() {
        assertTrue(JavaPlugin.class.isAssignableFrom(MasonryPlugin.class));
        assertTrue(WorldAccess.class.isAssignableFrom(PaperWorldAccess.class));
        assertTrue(SurvivalTransaction.class.isAssignableFrom(PaperSurvivalTransaction.class));
        assertTrue(PreviewRenderer.class.isAssignableFrom(PaperPreviewRenderer.class));
        assertTrue(TaskScheduler.class.isAssignableFrom(PaperTaskScheduler.class));
        assertTrue(PermissionService.class.isAssignableFrom(PaperPermissionService.class));
    }

    @Test
    void previewPlanUsesCompleteFacesWithinBudget() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 63, 80, 63));
        var planned = PaperPreviewRenderer.plan(selection);

        int width = 64;
        int height = 17;
        int depth = 64;
        int expectedSurface = 2 * (width * depth + width * height + height * depth)
                - 4 * (width + height + depth) + 8;
        assertEquals(expectedSurface, planned.size());
        assertTrue(planned.size() < selection.volume());
        assertTrue(planned.contains(new BlockPosition("world", 1, 64, 1)));
        assertTrue(planned.contains(new BlockPosition("world", 1, 70, 0)));
        assertTrue(planned.contains(new BlockPosition("world", 0, 70, 1)));
        assertTrue(!planned.contains(new BlockPosition("world", 1, 70, 1)));
        assertEquals(planned.size(), planned.stream().distinct().count());
    }
}
