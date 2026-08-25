package dev.mintychochip.masonry.common.support;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import java.util.ArrayList;
import java.util.List;

/** Records {@link #show} and {@link #showSelection} calls for assertions. */
public final class RecordingPreviewRenderer implements PreviewRenderer {
    private final List<CuboidSelection> selections = new ArrayList<>();
    private final List<ToolPreview> previews = new ArrayList<>();

    @Override
    public void show(ActorId actor, ToolPreview preview) {
        previews.add(preview);
    }

    @Override
    public void showSelection(ActorId actor, CuboidSelection selection) {
        selections.add(selection);
    }

    @Override
    public void clear(ActorId actor) {
        selections.clear();
        previews.clear();
    }

    public List<CuboidSelection> selections() {
        return List.copyOf(selections);
    }

    public List<ToolPreview> previews() {
        return List.copyOf(previews);
    }
}
