package dev.mintychochip.masonry.common.operation;

import dev.mintychochip.masonry.api.limits.OperationLimits;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import dev.mintychochip.masonry.api.world.BlockPosition;
import java.util.Objects;

/**
 * Enforces interaction distance, selection extent, and max operation size independently.
 */
public final class OperationGuard {
    private final OperationLimits limits;

    /**
     * @param limits positive limits
     */
    public OperationGuard(OperationLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    /** @return the limits this guard applies */
    public OperationLimits limits() {
        return limits;
    }

    /**
     * Checks that {@code target} is within {@code interactionDistance} of {@code origin}.
     *
     * @param origin player feet/eye block
     * @param target looked-at or specified block
     * @return valid, or an interaction error
     */
    public ValidationResult validateInteraction(BlockPosition origin, BlockPosition target) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(target, "target");
        if (!origin.worldId().equals(target.worldId())) {
            return ValidationResult.invalid("Interaction must stay in the same world");
        }
        if (origin.centerDistance(target) > limits.interactionDistance()) {
            return ValidationResult.invalid("Target is beyond interaction distance");
        }
        return ValidationResult.passed();
    }

    /**
     * Checks selection extent only. Volume is not capped here so {@code max_operation_blocks}
     * stays independent.
     *
     * @param selection cuboid
     * @return valid, or an extent error
     */
    public ValidationResult validateSelection(CuboidSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection.extent() > limits.selectionExtent()) {
            return ValidationResult.invalid("Selection exceeds maximum extent");
        }
        return ValidationResult.passed();
    }

    /**
     * Checks the preview region extent and that {@code affectedCount} is within
     * {@code maxOperationBlocks}.
     *
     * @param preview planned mutation
     * @return valid, or a size error
     */
    public ValidationResult validatePreview(ToolPreview preview) {
        Objects.requireNonNull(preview, "preview");
        ValidationResult selection = validateSelection(preview.region());
        if (!selection.valid()) {
            return selection;
        }
        if (preview.affectedCount() > limits.maxOperationBlocks()) {
            return ValidationResult.invalid("Preview exceeds maximum operation size");
        }
        return ValidationResult.passed();
    }
}
