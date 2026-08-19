package dev.mintychochip.buildtools.api.cost;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable multiset of item ids to non-negative counts (placement charge or harvest).
 *
 * @param itemCounts unmodifiable map of namespaced item key to count
 */
public record ResourceCost(Map<String, Integer> itemCounts) {
    /**
     * @throws NullPointerException if {@code itemCounts} or a key is {@code null}
     * @throws IllegalArgumentException if any count is negative
     */
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

    /** @return an empty cost */
    public static ResourceCost none() {
        return new ResourceCost(Map.of());
    }

    /**
     * @param itemKey namespaced item id
     * @param count number of items; {@code 0} yields {@link #none()}
     * @return single-item cost
     */
    public static ResourceCost of(String itemKey, int count) {
        if (count == 0) {
            return none();
        }
        return new ResourceCost(Map.of(itemKey, count));
    }

    /** @return {@code true} if there are no positive counts */
    public boolean isEmpty() {
        return itemCounts.isEmpty() || itemCounts.values().stream().allMatch(count -> count == 0);
    }

    /**
     * Adds another cost, summing counts per item and dropping zeros.
     *
     * @param other other cost
     * @return combined cost
     */
    public ResourceCost plus(ResourceCost other) {
        Objects.requireNonNull(other, "other");
        Map<String, Integer> merged = new LinkedHashMap<>(itemCounts);
        other.itemCounts.forEach((key, count) -> merged.merge(key, count, Integer::sum));
        merged.values().removeIf(count -> count == 0);
        return new ResourceCost(merged);
    }

    /**
     * @param itemKey namespaced item id
     * @return count of that item, or {@code 0}
     */
    public int countOf(String itemKey) {
        return itemCounts.getOrDefault(itemKey, 0);
    }

    /** @return sum of all item counts */
    public int totalItems() {
        return itemCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
}
