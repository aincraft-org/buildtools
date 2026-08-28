package dev.mintychochip.masonry.api.service;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.world.BlockPosition;

/**
 * Shows a bounded selection or tool outline to one player using client-only display packets or particles.
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
     * Shows the clipboard volume as client-only BlockDisplay packets at {@code origin}, so the player can
     * see what a paste/move would place before committing. Transparent cells stay see-through.
     * Bounded to a display packet cap.
     *
     * @param actor viewer
     * @param clipboard copied volume
     * @param origin paste origin
     */
    void showGhost(ActorId actor, Clipboard clipboard, BlockPosition origin);

    /**
     * Clears that player's client-side preview state.
     *
     * @param actor viewer
     */
    void clear(ActorId actor);
}
