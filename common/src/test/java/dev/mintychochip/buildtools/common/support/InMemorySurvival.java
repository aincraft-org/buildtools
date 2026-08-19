package dev.mintychochip.buildtools.common.support;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Counted inventory for tests. Refund overflow beyond {@link #withCapacity(int)} is recorded
 * as drops. Creative and bypass skip charge/refund.
 */
public final class InMemorySurvival implements SurvivalTransaction {
    private final Map<ActorId, Map<String, Integer>> inventories = new HashMap<>();
    private final List<ResourceCost> charges = new ArrayList<>();
    private final List<ResourceCost> refunds = new ArrayList<>();
    private final List<ResourceCost> drops = new ArrayList<>();
    private final Set<ActorId> creative = new HashSet<>();
    private final Set<ActorId> bypass = new HashSet<>();
    private int inventoryCapacity = Integer.MAX_VALUE;

    /**
     * Adds items without respecting capacity.
     *
     * @param actor player
     * @param item item key
     * @param count amount
     * @return {@code this}
     */
    public InMemorySurvival give(ActorId actor, String item, int count) {
        inventories.computeIfAbsent(actor, ignored -> new HashMap<>()).merge(item, count, Integer::sum);
        return this;
    }

    /** @param actor player treated as creative @return {@code this} */
    public InMemorySurvival creative(ActorId actor) {
        creative.add(actor);
        return this;
    }

    /** @param actor player with survival bypass @return {@code this} */
    public InMemorySurvival bypass(ActorId actor) {
        bypass.add(actor);
        return this;
    }

    /**
     * Caps how many items refund can place in inventory; the rest become drops.
     *
     * @param capacity max items
     * @return {@code this}
     */
    public InMemorySurvival withCapacity(int capacity) {
        this.inventoryCapacity = capacity;
        return this;
    }

    public int count(ActorId actor, String item) {
        return inventories.getOrDefault(actor, Map.of()).getOrDefault(item, 0);
    }

    public List<ResourceCost> charges() {
        return List.copyOf(charges);
    }

    public List<ResourceCost> refunds() {
        return List.copyOf(refunds);
    }

    public List<ResourceCost> drops() {
        return List.copyOf(drops);
    }

    @Override
    public boolean bypassesCost(ActorId actor) {
        return creative.contains(actor) || bypass.contains(actor);
    }

    @Override
    public boolean canAfford(ActorId actor, ResourceCost cost) {
        if (bypassesCost(actor) || cost.isEmpty()) {
            return true;
        }
        Map<String, Integer> inventory = inventories.getOrDefault(actor, Map.of());
        for (var entry : cost.itemCounts().entrySet()) {
            if (inventory.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void charge(ActorId actor, ResourceCost cost) {
        if (bypassesCost(actor) || cost.isEmpty()) {
            return;
        }
        if (!canAfford(actor, cost)) {
            throw new IllegalStateException("cannot afford " + cost);
        }
        Map<String, Integer> inventory = inventories.computeIfAbsent(actor, ignored -> new HashMap<>());
        cost.itemCounts().forEach((item, count) -> inventory.merge(item, -count, Integer::sum));
        charges.add(cost);
    }

    @Override
    public void refund(ActorId actor, ResourceCost cost) {
        if (bypassesCost(actor) || cost.isEmpty()) {
            return;
        }
        refunds.add(cost);
        Map<String, Integer> inventory = inventories.computeIfAbsent(actor, ignored -> new HashMap<>());
        int used = inventory.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Integer> dropped = new HashMap<>();
        for (var entry : cost.itemCounts().entrySet()) {
            int remaining = entry.getValue();
            int space = Math.max(0, inventoryCapacity - used);
            int toInventory = Math.min(space, remaining);
            if (toInventory > 0) {
                inventory.merge(entry.getKey(), toInventory, Integer::sum);
                used += toInventory;
                remaining -= toInventory;
            }
            if (remaining > 0) {
                dropped.merge(entry.getKey(), remaining, Integer::sum);
            }
        }
        if (!dropped.isEmpty()) {
            drops.add(new ResourceCost(dropped));
        }
    }
}
