package dev.mintychochip.masonry.common.session;

import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.preview.PreviewMode;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Per-player pos1/pos2, unnamed clipboard, tool mode, preview mode, and the last executed
 * tool (for repeat). A selection exists only when both corners are set.
 */
public final class PlayerSession {
    private BlockPosition pos1;
    private BlockPosition pos2;
    private Clipboard clipboard;
    private ToolMode mode = ToolMode.FILL;
    private PreviewMode previewMode = PreviewMode.BLOCK_LIGHT_BLUE;
    private boolean previewAnimation = true;
    private String lastTool;
    private Map<String, String> lastArgs = Map.of();
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

    /** Clears both selection corners, keeping clipboard, mode, and preview mode. */
    public void clearSelection() {
        pos1 = null;
        pos2 = null;
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

    /** @return current tool mode */
    public ToolMode mode() {
        return mode;
    }

    /** @param mode new tool mode */
    public void setMode(ToolMode mode) {
        this.mode = Objects.requireNonNull(mode, "mode");
    }

    /** @return current preview mode */
    public PreviewMode previewMode() {
        return previewMode;
    }

    /** @param previewMode new preview mode */
    public void setPreviewMode(PreviewMode previewMode) {
        this.previewMode = Objects.requireNonNull(previewMode, "previewMode");
    }

    /** @return whether block-display preview animation is enabled */
    public boolean previewAnimation() {
        return previewAnimation;
    }

    /** @param previewAnimation whether to animate preview block displays */
    public void setPreviewAnimation(boolean previewAnimation) {
        this.previewAnimation = previewAnimation;
    }

    public String lastTool() {
        return lastTool;
    }

    /** @return arguments of the last executed tool, possibly empty */
    public Map<String, String> lastArgs() {
        return lastArgs;
    }

    /** @param toolName last executed tool name, or {@code null} to clear */
    public void setLastTool(String toolName, Map<String, String> args) {
        this.lastTool = toolName;
        this.lastArgs = args == null ? Map.of() : Map.copyOf(args);
    }

    /** Clears corners, clipboard, mode, and preview mode. */
    public void clear() {
        pos1 = null;
        pos2 = null;
        clipboard = null;
        mode = ToolMode.FILL;
        previewMode = PreviewMode.BLOCK_LIGHT_BLUE;
        lastTool = null;
        lastArgs = Map.of();
    }
}
