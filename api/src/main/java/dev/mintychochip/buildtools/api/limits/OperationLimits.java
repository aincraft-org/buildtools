package dev.mintychochip.buildtools.api.limits;

public record OperationLimits(int interactionDistance, int selectionExtent, int maxOperationBlocks) {
    public OperationLimits {
        if (interactionDistance <= 0 || selectionExtent <= 0 || maxOperationBlocks <= 0) {
            throw new IllegalArgumentException("All operation limits must be positive");
        }
    }

    public static OperationLimits defaults() {
        return new OperationLimits(6, 64, 32_768);
    }
}
