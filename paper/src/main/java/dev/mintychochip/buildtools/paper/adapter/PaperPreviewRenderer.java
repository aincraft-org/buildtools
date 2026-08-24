package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.preview.PreviewMode;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import dev.mintychochip.buildtools.api.service.PreviewRenderer;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.world.BlockPosition;
import dev.mintychochip.buildtools.common.preview.PreviewGeometry;
import dev.mintychochip.buildtools.common.session.PlayerSessionStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Multi-mode preview renderer. Viable modes are translucent-glass {@link BlockDisplay},
 * six-face alpha {@link TextDisplay}, and particle outline.
 */
public final class PaperPreviewRenderer implements PreviewRenderer {
    static final int MAX_DISPLAYS = PreviewGeometry.DEFAULT_MAX_DISPLAYS;

    private final JavaPlugin plugin;
    private final Server server;
    private final PlayerSessionStore sessions;
    private final Map<ActorId, List<UUID>> spawned = new HashMap<>();

    public PaperPreviewRenderer(JavaPlugin plugin, PlayerSessionStore sessions) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.sessions = Objects.requireNonNull(sessions, "sessions");
    }
    /**
     * Plans a bounded outline for {@code selection}. Exposed for unit tests without a server.
     */
    public static List<BlockPosition> plan(CuboidSelection selection) {
        return PreviewGeometry.outline(selection, MAX_DISPLAYS);
    }

    @Override
    public void show(ActorId actor, ToolPreview preview) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(preview, "preview");
        render(actor, preview.region());
    }

    @Override
    public void showSelection(ActorId actor, CuboidSelection selection) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(selection, "selection");
        render(actor, selection);
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

    private void render(ActorId actor, CuboidSelection region) {
        clear(actor);
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return;
        }
        World world = server.getWorld(region.worldId());
        if (world == null) {
            return;
        }
        PreviewMode mode = sessions.session(actor).previewMode();
        switch (mode) {
            case TEXT_LOW -> showTextCube(actor, region, world, player, Color.fromARGB(64, 0, 170, 255));
            case TEXT_HIGH -> showTextCube(actor, region, world, player, Color.fromARGB(180, 0, 170, 255));
            case PARTICLE -> showParticles(actor, region, world, player);
            default -> showBlockDisplays(actor, region, world, player, mode);
        }
    }

    private void showTextCube(ActorId actor, CuboidSelection region, World world, Player player, Color color) {
        BlockPosition min = region.min();
        BlockPosition max = region.max();
        List<UUID> ids = new ArrayList<>();

        // Six faces; face centers are at the outer boundary of the inclusive block box.
        float xMid = (min.x() + max.x() + 1) / 2.0f;
        float yMid = (min.y() + max.y() + 1) / 2.0f;
        float zMid = (min.z() + max.z() + 1) / 2.0f;

        float wX = region.width();
        float hY = region.height();
        float dZ = region.depth();

        Face[] faces = {
                new Face(new Location(world, min.x(), yMid, zMid), dZ, hY),         // -X
                new Face(new Location(world, max.x() + 1, yMid, zMid), dZ, hY),    // +X
                new Face(new Location(world, xMid, min.y(), zMid), wX, dZ),        // -Y
                new Face(new Location(world, xMid, max.y() + 1, zMid), wX, dZ),    // +Y
                new Face(new Location(world, xMid, yMid, min.z()), wX, hY),        // -Z
                new Face(new Location(world, xMid, yMid, max.z() + 1), wX, hY)     // +Z
        };

        for (Face face : faces) {
            try {
                TextDisplay display = world.spawn(face.location(), TextDisplay.class, entity -> {
                    entity.text(Component.empty());
                    entity.setDefaultBackground(false);
                    entity.setBackgroundColor(color);
                    entity.setTextOpacity((byte) 0);
                    entity.setSeeThrough(true);
                    entity.setBillboard(Display.Billboard.CENTER);
                    entity.setDisplayWidth(face.width);
                    entity.setDisplayHeight(face.height);
                    entity.setPersistent(false);
                    entity.setVisibleByDefault(false);
                });
                player.showEntity(plugin, display);
                ids.add(display.getUniqueId());
            } catch (RuntimeException ignored) {
                // fall through to no preview
            }
        }
        spawned.put(actor, ids);
    }

    private void showBlockDisplays(ActorId actor, CuboidSelection region, World world, Player player, PreviewMode mode) {
        String block = switch (mode) {
            case BLOCK_TINTED -> "minecraft:tinted_glass";
            case BLOCK_CLEAR -> "minecraft:glass";
            default -> "minecraft:light_blue_stained_glass";
        };
        BlockData data = server.createBlockData(block);
        List<BlockPosition> points = PreviewGeometry.outline(region, MAX_DISPLAYS);
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
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        if (usedParticles && ids.isEmpty()) {
            for (BlockPosition point : points) {
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        spawned.put(actor, ids);
    }

    private void showParticles(ActorId actor, CuboidSelection region, World world, Player player) {
        List<BlockPosition> points = PreviewGeometry.outline(region, MAX_DISPLAYS);
        for (BlockPosition point : points) {
            spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
        }
        spawned.put(actor, List.of());
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

    private record Face(Location location, float width, float height) {}
}
