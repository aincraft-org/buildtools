package dev.mintychochip.buildtools.common.operation;

import dev.mintychochip.buildtools.api.limits.OperationLimits;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import java.util.Objects;

public final class OperationGuard {
    private final OperationLimits limits;

    public OperationGuard(OperationLimits limits) {
        this.limits = Objects.requireNonNull(limits, "limits");
    }

    public OperationLimits limits() {
        return limits;
    }

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

    public ValidationResult validateSelection(CuboidSelection selection) {
        Objects.requireNonNull(selection, "selection");
        if (selection.extent() > limits.selectionExtent()) {
            return ValidationResult.invalid("Selection exceeds maximum extent");
        }
        return ValidationResult.passed();
    }

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
