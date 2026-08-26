package dev.mintychochip.masonry.common.tool;

import dev.mintychochip.masonry.api.cost.ResourceCost;
import dev.mintychochip.masonry.api.operation.OperationRecord;
import dev.mintychochip.masonry.api.service.ClipboardHolder;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.tool.ToolRequest;
import dev.mintychochip.masonry.api.tool.ValidationResult;

/**
 * Cut for the move gesture: copies the selection into the clipboard and clears it to air.
 * The gadget's MOVE mode then pastes the held clipboard at the new origin, so a block move
 * is a copy+clear followed by a paste. Undo of the move clears the pasted cells and restores
 * the cut cells via the two recorded operations.
 */
public final class MoveTool extends CutTool {
    /**
     * @param clipboards session clipboard port
     */
    public MoveTool(ClipboardHolder clipboards) {
        super(clipboards);
    }

    @Override
    public String name() {
        return "move";
    }

    @Override
    public ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        return super.validate(request, world, survival);
    }

    @Override
    public ToolPreview preview(ToolRequest request, WorldAccess world) {
        return super.preview(request, world);
    }

    @Override
    public OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival) {
        return super.execute(request, world, survival);
    }
}
