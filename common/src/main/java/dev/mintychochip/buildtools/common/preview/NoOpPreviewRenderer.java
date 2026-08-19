package dev.mintychochip.buildtools.common.preview;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import java.util.Objects;

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
