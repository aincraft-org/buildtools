package dev.mintychochip.buildtools.common.session;

import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.Optional;

/**
 * Per-player pos1/pos2 and unnamed clipboard. A selection exists only when both corners are set.
 */
public final class PlayerSession {
    private BlockPosition pos1;
    private BlockPosition pos2;
    private Clipboard clipboard;

    /** @return first corner if set */
    public Optional<BlockPosition> pos1() {
        return Optional.ofNullable(pos1);
    }

    /** @return second corner if set */
    public Optional<BlockPosition> pos2() {
        return Optional.ofNullable(pos2);
    }

    /** @param position first corner */
    public void setPos1(BlockPosition position) {
        this.pos1 = position;
    }

    /** @param position second corner */
    public void setPos2(BlockPosition position) {
        this.pos2 = position;
    }

    /** @return cuboid when both corners are set */
    public Optional<CuboidSelection> selection() {
        if (pos1 == null || pos2 == null) {
            return Optional.empty();
        }
        return Optional.of(new CuboidSelection(pos1, pos2));
    }

    /** @return current clipboard if any */
    public Optional<Clipboard> clipboard() {
        return Optional.ofNullable(clipboard);
    }

    /** @param clipboard new clipboard, or {@code null} to clear */
    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    /** Clears corners and clipboard. */
    public void clear() {
        pos1 = null;
        pos2 = null;
        clipboard = null;
    }
}
