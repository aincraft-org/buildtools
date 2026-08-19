package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;

/**
 * Inventory charge and refund. Creative and bypass nodes skip cost via {@link #bypassesCost}.
 */
public interface SurvivalTransaction {
    /**
     * @param actor player
     * @param cost required items
     * @return {@code true} if the actor can pay (or the cost is empty / bypassed)
     */
    boolean canAfford(ActorId actor, ResourceCost cost);

    /**
     * Removes items. Implementations no-op when {@link #bypassesCost} is {@code true}.
     *
     * @param actor player
     * @param cost items to remove
     * @throws IllegalStateException if the actor cannot afford {@code cost}
     */
    void charge(ActorId actor, ResourceCost cost);

    /**
     * Adds items back. Leftovers that do not fit the inventory must be dropped at the player.
     *
     * @param actor player
     * @param cost items to give
     */
    void refund(ActorId actor, ResourceCost cost);

    /**
     * @param actor player
     * @return {@code true} if creative or {@code buildtools.bypass.creative}/{@code .survival}
     */
    default boolean bypassesCost(ActorId actor) {
        return false;
    }
}
