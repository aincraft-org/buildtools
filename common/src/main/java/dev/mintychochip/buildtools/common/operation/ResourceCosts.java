package dev.mintychochip.buildtools.common.operation;

import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.operation.BlockChange;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ResourceCosts {
    private ResourceCosts() {}

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
