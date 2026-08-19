package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.preview.PreviewGeometry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class PaperPreviewRenderer implements PreviewRenderer {
    static final int MAX_DISPLAYS = PreviewGeometry.DEFAULT_MAX_DISPLAYS;

    private final JavaPlugin plugin;
    private final Server server;
    private final Map<ActorId, List<UUID>> spawned = new HashMap<>();

    public PaperPreviewRenderer(JavaPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
    }

    @Override
    public void show(ActorId actor, ToolPreview preview) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(preview, "preview");
        showPositions(actor, preview.region(), plan(preview.region()));
    }

    @Override
    public void showSelection(ActorId actor, CuboidSelection selection) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(selection, "selection");
        showPositions(actor, selection, plan(selection));
    }

    public static List<BlockPosition> plan(CuboidSelection selection) {
        return PreviewGeometry.outline(selection, MAX_DISPLAYS);
    }

    @Override
    public void clear(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        List<UUID> ids = spawned.remove(actor);
        if (ids == null) {
            return;
        }
        for (UUID id : ids) {
            Entity entity = server.getEntity(id);
            if (entity != null) {
                entity.remove();
            }
        }
    }

    private void showPositions(ActorId actor, CuboidSelection region, List<BlockPosition> points) {
        clear(actor);
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return;
        }
        World world = server.getWorld(region.worldId());
        if (world == null) {
            return;
        }
        BlockData data = server.createBlockData("minecraft:light_blue_stained_glass");
        List<UUID> ids = new ArrayList<>();
        boolean usedParticles = false;
        for (BlockPosition point : points) {
            Location location = new Location(world, point.x(), point.y(), point.z());
            try {
                BlockDisplay display = world.spawn(location, BlockDisplay.class, entity -> {
                    entity.setBlock(data);
                    entity.setPersistent(false);
                    entity.setTransformation(new Transformation(
                            new Vector3f(0.05f, 0.05f, 0.05f),
                            new AxisAngle4f(),
                            new Vector3f(0.9f, 0.9f, 0.9f),
                            new AxisAngle4f()));
                    entity.setVisibleByDefault(false);
                });
                player.showEntity(plugin, display);
                ids.add(display.getUniqueId());
            } catch (RuntimeException ignored) {
                usedParticles = true;
                spawnParticle(player, location);
            }
        }
        if (usedParticles && ids.isEmpty()) {
            for (BlockPosition point : points) {
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        spawned.put(actor, ids);
    }

    private static void spawnParticle(Player player, Location location) {
        player.spawnParticle(
                Particle.DUST,
                location,
                1,
                0,
                0,
                0,
                0,
                new Particle.DustOptions(Color.AQUA, 1.0f));
    }
}
