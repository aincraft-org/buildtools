package dev.mintychochip.masonry.api.limits;

/**
 * Server limits that must be checked independently.
 *
 * @param interactionDistance max raycast/target distance when setting a corner
 * @param selectionExtent max inclusive edge length of a cuboid
 * @param maxOperationBlocks max blocks a single tool preview may affect
 */
public record OperationLimits(int interactionDistance, int selectionExtent, int maxOperationBlocks) {
    /**
     * @throws IllegalArgumentException if any value is not positive
     */
    public OperationLimits {
        if (interactionDistance <= 0 || selectionExtent <= 0 || maxOperationBlocks <= 0) {
            throw new IllegalArgumentException("All operation limits must be positive");
        }
    }

    /**
     * Locked V1 defaults: reach 6, extent 64, 32_768 operation blocks.
     *
     * @return default limits
     */
    public static OperationLimits defaults() {
        return new OperationLimits(6, 64, 32_768);
    }
}
