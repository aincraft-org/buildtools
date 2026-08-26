package dev.mintychochip.masonry.api.service;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.world.BlockPosition;

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
     * Shows the clipboard volume as a translucent preview at {@code origin}, so players can
     * see what a paste/move would place before committing. Transparent cells stay
     * see-through. Bounded to a display cap.
     *
     * @param actor viewer
     * @param clipboard copied volume
     * @param origin paste origin
     */
    void showGhost(ActorId actor, Clipboard clipboard, BlockPosition origin);

    /**
     * Removes that player's preview entities.
     *
     * @param actor viewer
     */
    void clear(ActorId actor);
}
