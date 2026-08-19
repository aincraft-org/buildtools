package dev.mintychochip.buildtools.common.support;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import java.util.ArrayList;
import java.util.List;

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
