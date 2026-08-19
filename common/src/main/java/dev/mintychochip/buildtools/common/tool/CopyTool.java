package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.clipboard.BlockOffset;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.ClipboardHolder;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;
import dev.mintychochip.buildtools.api.tool.Tool;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ToolRequest;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Copies the selection (including air) into the actor clipboard. Does not mutate the world;
 * preview affected set is empty so the executor does not treat it as a cancelled mutation.
 */
public final class CopyTool implements Tool {
    private final ClipboardHolder clipboards;

    /**
     * @param clipboards session clipboard port
     */
    public CopyTool(ClipboardHolder clipboards) {
        this.clipboards = Objects.requireNonNull(clipboards, "clipboards");
    }

    @Override
    public String name() {
        return "copy";
    }

    @Override
    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        if (request.selection() == null) {
            return new ToolPreview(
                    new dev.mintychochip.buildtools.api.selection.CuboidSelection(
                            new BlockPosition("world", 0, 0, 0), new BlockPosition("world", 0, 0, 0)),
                    java.util.List.of(),
                    ResourceCost.none());
        }
        return new ToolPreview(request.selection(), java.util.List.of(), ResourceCost.none());
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        if (request.selection() == null) {
            return ValidationResult.invalid("A valid selection is required");
        }
        return ValidationResult.passed();
    }

    @Override
    public OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        BlockPosition origin = request.selection().min();
        Map<BlockOffset, dev.mintychochip.buildtools.api.world.BlockState> blocks = new LinkedHashMap<>();
        for (BlockPosition position : request.selection().positions()) {
            blocks.put(
                    new BlockOffset(
                            position.x() - origin.x(),
                            position.y() - origin.y(),
                            position.z() - origin.z()),
                    world.getBlock(position));
        }
        clipboards.setClipboard(request.actorId(), new Clipboard(origin.worldId(), blocks));
        return new OperationRecord(
                UUID.randomUUID(), request.actorId(), name(), java.util.List.of(), ResourceCost.none());
    }

    @Override
    public void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival) {
        // Copy does not mutate the world.
    }
}
