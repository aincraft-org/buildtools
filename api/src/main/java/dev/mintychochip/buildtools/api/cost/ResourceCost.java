package dev.mintychochip.buildtools.api.cost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public record ResourceCost(Map<String, Integer> itemCounts) {
    public ResourceCost {
        Objects.requireNonNull(itemCounts, "itemCounts");
        itemCounts.forEach((key, count) -> {
            Objects.requireNonNull(key, "item key");
            if (count == null || count < 0) {
                throw new IllegalArgumentException("Item count must be >= 0: " + key);
            }
        });
        itemCounts = Map.copyOf(itemCounts);
    }

    public static ResourceCost none() {
        return new ResourceCost(Map.of());
    }

    public static ResourceCost of(String itemKey, int count) {
        if (count == 0) {
            return none();
        }
        return new ResourceCost(Map.of(itemKey, count));
    }

    public boolean isEmpty() {
        return itemCounts.isEmpty() || itemCounts.values().stream().allMatch(count -> count == 0);
    }

    public ResourceCost plus(ResourceCost other) {
        Objects.requireNonNull(other, "other");
        Map<String, Integer> merged = new LinkedHashMap<>(itemCounts);
        other.itemCounts.forEach((key, count) -> merged.merge(key, count, Integer::sum));
        merged.values().removeIf(count -> count == 0);
        return new ResourceCost(merged);
    }

    public int countOf(String itemKey) {
        return itemCounts.getOrDefault(itemKey, 0);
    }

    public int totalItems() {
        return itemCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
