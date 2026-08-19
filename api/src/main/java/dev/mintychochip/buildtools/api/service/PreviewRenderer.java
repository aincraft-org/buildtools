package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolPreview;

/**
 * Shows a bounded selection or tool outline to one player. Never one display per volume block.
 */
public interface PreviewRenderer {
    /**
     * Shows a tool preview outline.
     *
     * @param actor viewer
     * @param preview planned region
     */
    void show(ActorId actor, ToolPreview preview);

    /**
     * Shows the active selection outline.
     *
     * @param actor viewer
     * @param selection cuboid
     */
    void showSelection(ActorId actor, CuboidSelection selection);

    /**
     * Removes that player's preview entities.
     *
     * @param actor viewer
     */
    void clear(ActorId actor);
}
