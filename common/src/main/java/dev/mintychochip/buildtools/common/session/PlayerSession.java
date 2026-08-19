package dev.mintychochip.buildtools.common.session;

import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.Optional;

public final class PlayerSession {
    private BlockPosition pos1;
    private BlockPosition pos2;
    private Clipboard clipboard;

    public Optional<BlockPosition> pos1() {
        return Optional.ofNullable(pos1);
    }

    public Optional<BlockPosition> pos2() {
        return Optional.ofNullable(pos2);
    }

    public void setPos1(BlockPosition position) {
        this.pos1 = position;
    }

    public void setPos2(BlockPosition position) {
        this.pos2 = position;
    }

    public Optional<CuboidSelection> selection() {
        if (pos1 == null || pos2 == null) {
            return Optional.empty();
        }
        return Optional.of(new CuboidSelection(pos1, pos2));
    }

    public Optional<Clipboard> clipboard() {
        return Optional.ofNullable(clipboard);
    }

    public void setClipboard(Clipboard clipboard) {
        this.clipboard = clipboard;
    }

    public void clear() {
        pos1 = null;
        pos2 = null;
        clipboard = null;
    }
}
