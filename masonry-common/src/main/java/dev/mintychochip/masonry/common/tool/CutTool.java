package dev.mintychochip.masonry.common.tool;

import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.cost.ResourceCost;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.operation.OperationRecord;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.ClipboardHolder;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.tool.Tool;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Copies the selection into the actor clipboard and clears the copied cells to air. The
 * recorded diff is the cleared cells, so undo restores them.
 */
public class CutTool implements Tool {
    private final ClipboardHolder clipboards;

    /**
     * @param clipboards session clipboard port
     */
    public CutTool(ClipboardHolder clipboards) {
        this.clipboards = Objects.requireNonNull(clipboards, "clipboards");
    }

    @Override
    public String name() {
        return "cut";
    }

    @Override
    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        if (request.selection() == null) {
            return new ToolPreview(null, List.of(), ResourceCost.none());
        }
        return new ToolPreview(request.selection(), clearedPositions(request, world), ResourceCost.none());
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
        CuboidSelection selection = request.selection();
        BlockPosition origin = selection.min();
        Map<BlockOffset, BlockState> blocks = new LinkedHashMap<>();
        for (BlockPosition position : selection.positions()) {
            blocks.put(
                    new BlockOffset(
                            position.x() - origin.x(),
                            position.y() - origin.y(),
                            position.z() - origin.z()),
                    world.getBlock(position));
        }
        clipboards.setClipboard(request.actorId(), new Clipboard(origin.worldId(), blocks));

        List<BlockChange> cleared = new ArrayList<>();
        for (BlockPosition position : selection.positions()) {
            if (request.isExcluded(position)) {
                continue;
            }
            BlockState before = world.getBlock(position);
            if (!before.isAir()) {
                cleared.add(new BlockChange(position, before, BlockState.AIR));
            }
        }
        if (!world.setBlocks(request.actorId(), cleared)) {
            return new OperationRecord(UUID.randomUUID(), request.actorId(), name(), List.of(),
                    ResourceCost.none());
        }
        return new OperationRecord(UUID.randomUUID(), request.actorId(), name(), cleared,
                ResourceCost.none());
    }

    @Override
    public void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival) {
        List<BlockChange> changes = record.changes();
        for (int i = changes.size() - 1; i >= 0; i--) {
            BlockChange change = changes.get(i);
            world.setBlock(record.actorId(), change.position(), change.before());
        }
    }

    private static List<BlockPosition> clearedPositions(ToolRequest request, WorldAccess world) {
        List<BlockPosition> cleared = new ArrayList<>();
        for (BlockPosition position : request.selection().positions()) {
            if (request.isExcluded(position)) {
                continue;
            }
            if (!world.getBlock(position).isAir()) {
                cleared.add(position);
            }
        }
        return List.copyOf(cleared);
    }
}
