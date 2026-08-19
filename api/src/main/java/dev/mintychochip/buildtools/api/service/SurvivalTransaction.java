package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;

public interface SurvivalTransaction {
    boolean canAfford(ActorId actor, ResourceCost cost);

    void charge(ActorId actor, ResourceCost cost);

    void refund(ActorId actor, ResourceCost cost);

    default boolean bypassesCost(ActorId actor) {
        return false;
    }
}
