package dev.mintychochip.masonry.common.support;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.ArrayList;
import java.util.List;

/** Records {@link #show}, {@link #showSelection}, and {@link #showGhost} calls for assertions. */
public final class RecordingPreviewRenderer implements PreviewRenderer {
    private final List<CuboidSelection> selections = new ArrayList<>();
    private final List<ToolPreview> previews = new ArrayList<>();
    private final List<Clipboard> ghosts = new ArrayList<>();
    private final List<BlockPosition> ghostOrigins = new ArrayList<>();

    @Override
    public void show(ActorId actor, ToolPreview preview) {
        previews.add(preview);
    }

    @Override
    public void showSelection(ActorId actor, CuboidSelection selection) {
        selections.add(selection);
    }

    @Override
    public void showGhost(ActorId actor, Clipboard clipboard, BlockPosition origin) {
        ghosts.add(clipboard);
        ghostOrigins.add(origin);
    }

    @Override
    public void clear(ActorId actor) {
        selections.clear();
        previews.clear();
        ghosts.clear();
        ghostOrigins.clear();
    }

    public List<CuboidSelection> selections() {
        return List.copyOf(selections);
    }

    public List<ToolPreview> previews() {
        return List.copyOf(previews);
    }

    public List<Clipboard> ghosts() {
        return List.copyOf(ghosts);
    }

    public List<BlockPosition> ghostOrigins() {
        return List.copyOf(ghostOrigins);
    }
}
