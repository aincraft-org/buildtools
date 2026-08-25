package dev.mintychochip.masonry.common.preview;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import java.util.Objects;

/**
 * Preview renderer that only rejects {@code null}. Used in JVM tests and headless wiring.
 */
public final class NoOpPreviewRenderer implements PreviewRenderer {
    @Override
    public void show(ActorId actor, ToolPreview preview) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(preview, "preview");
    }

    @Override
    public void showSelection(ActorId actor, CuboidSelection selection) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(selection, "selection");
    }

    @Override
    public void clear(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
    }
}
