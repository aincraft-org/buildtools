package dev.mintychochip.masonry.common.operation;

import dev.mintychochip.masonry.api.cost.ResourceCost;
import dev.mintychochip.masonry.api.operation.BlockChange;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Derives placement charge and harvest from recorded diffs.
 */
public final class ResourceCosts {
    private ResourceCosts() {}

    /**
     * One item per non-air {@code after} state that actually changed.
     *
     * @param changes diffs
     * @return placement cost
     */
    public static ResourceCost placementCost(List<BlockChange> changes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BlockChange change : changes) {
            if (change.isNoOp() || change.after().isAir()) {
                continue;
            }
            counts.merge(change.after().itemKey(), 1, Integer::sum);
        }
        return new ResourceCost(counts);
    }

    /**
     * One item per non-air {@code before} state that actually changed.
     *
     * @param changes diffs
     * @return harvest
     */
    public static ResourceCost harvest(List<BlockChange> changes) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (BlockChange change : changes) {
            if (change.isNoOp() || change.before().isAir()) {
                continue;
            }
            counts.merge(change.before().itemKey(), 1, Integer::sum);
        }
        return new ResourceCost(counts);
    }
}
