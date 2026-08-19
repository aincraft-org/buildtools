package dev.mintychochip.buildtools.paper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PermissionService;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.TaskScheduler;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.paper.adapter.PaperPermissionService;
import dev.mintychochip.buildtools.paper.adapter.PaperPreviewRenderer;
import dev.mintychochip.buildtools.paper.adapter.PaperSurvivalTransaction;
import dev.mintychochip.buildtools.paper.adapter.PaperTaskScheduler;
import dev.mintychochip.buildtools.paper.adapter.PaperWorldAccess;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.Test;

class PaperBoundaryTest {
    @Test
    void pluginMetadataPointsAtPaperEntryPoint() throws Exception {
        try (InputStream in = BuildToolsPlugin.class.getClassLoader().getResourceAsStream("plugin.yml")) {
            assertNotNull(in);
            String yaml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(yaml.contains("main: dev.mintychochip.buildtools.paper.BuildToolsPlugin"));
            assertTrue(yaml.contains("api-version: \"26.2\"")
                    || yaml.contains("api-version: '26.2'")
                    || yaml.contains("api-version: 26.2"));
            assertTrue(yaml.contains("name: BuildTools"));
            assertTrue(yaml.contains("bt"));
            assertTrue(yaml.contains("0.1.0"));
        }
    }

    @Test
    void pluginAndAdaptersStayOnThePaperSideOfTheBoundary() {
        assertTrue(JavaPlugin.class.isAssignableFrom(BuildToolsPlugin.class));
        assertTrue(WorldAccess.class.isAssignableFrom(PaperWorldAccess.class));
        assertTrue(SurvivalTransaction.class.isAssignableFrom(PaperSurvivalTransaction.class));
        assertTrue(PreviewRenderer.class.isAssignableFrom(PaperPreviewRenderer.class));
        assertTrue(TaskScheduler.class.isAssignableFrom(PaperTaskScheduler.class));
        assertTrue(PermissionService.class.isAssignableFrom(PaperPermissionService.class));
    }

    @Test
    void previewPlanIsBoundedOutlineNotPerBlock() {
        CuboidSelection selection = new CuboidSelection(
                new BlockPosition("world", 0, 64, 0),
                new BlockPosition("world", 63, 80, 63));
        var planned = PaperPreviewRenderer.plan(selection);
        assertTrue(planned.size() <= 256);
        assertTrue(planned.size() < selection.volume());
        assertTrue(planned.contains(selection.min()));
        assertEquals(planned.size(), planned.stream().distinct().count());
    }
}
