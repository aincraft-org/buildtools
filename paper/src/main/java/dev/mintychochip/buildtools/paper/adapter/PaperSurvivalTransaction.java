package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.cost.ResourceCost;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import java.util.HashMap;
import java.util.Objects;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class PaperSurvivalTransaction implements SurvivalTransaction {
    private final Server server;

    public PaperSurvivalTransaction(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean bypassesCost(ActorId actor) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return false;
        }
        return player.getGameMode() == GameMode.CREATIVE
                || player.hasPermission("buildtools.bypass.creative")
                || player.hasPermission("buildtools.bypass.survival");
    }

    @Override
    public boolean canAfford(ActorId actor, ResourceCost cost) {
        if (bypassesCost(actor) || cost.isEmpty()) {
            return true;
        }
        Player player = requirePlayer(actor);
        for (var entry : cost.itemCounts().entrySet()) {
            Material material = requireMaterial(entry.getKey());
            if (!player.getInventory().containsAtLeast(new ItemStack(material), entry.getValue())) {
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
        Player player = requirePlayer(actor);
        if (!canAfford(actor, cost)) {
            throw new IllegalStateException("Actor cannot afford " + cost);
        }
        for (var entry : cost.itemCounts().entrySet()) {
            player.getInventory().removeItem(new ItemStack(requireMaterial(entry.getKey()), entry.getValue()));
        }
    }

    @Override
    public void refund(ActorId actor, ResourceCost cost) {
        if (bypassesCost(actor) || cost.isEmpty()) {
            return;
        }
        Player player = requirePlayer(actor);
        for (var entry : cost.itemCounts().entrySet()) {
            HashMap<Integer, ItemStack> leftover =
                    player.getInventory().addItem(new ItemStack(requireMaterial(entry.getKey()), entry.getValue()));
            leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        }
    }

    private Player requirePlayer(ActorId actor) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            throw new IllegalArgumentException("Player is not online: " + actor.value());
        }
        return player;
    }

    private static Material requireMaterial(String key) {
        Material material = Material.matchMaterial(key);
        if (material == null || !material.isItem()) {
            throw new IllegalArgumentException("Unknown item: " + key);
        }
        return material;
    }
}
