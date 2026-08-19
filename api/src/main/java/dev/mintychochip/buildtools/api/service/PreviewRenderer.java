package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolPreview;

public interface PreviewRenderer {
    void show(ActorId actor, ToolPreview preview);

    void showSelection(ActorId actor, CuboidSelection selection);

    void clear(ActorId actor);
}
