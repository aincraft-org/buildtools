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
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

/**
 * Multi-mode preview renderer. Defaults to {@link BlockDisplay} with translucent glass; other
 * modes demonstrate TextDisplay, ItemDisplay, armor-stand heads, and particle outlines.
 */
public final class PaperPreviewRenderer implements PreviewRenderer {
    static final int MAX_DISPLAYS = PreviewGeometry.DEFAULT_MAX_DISPLAYS;

    private final JavaPlugin plugin;
    private final Server server;
    private final PlayerSessionStore sessions;
    private final Map<ActorId, List<UUID>> spawned = new HashMap<>();

    /**
     * @param plugin owning plugin
     * @param sessions player sessions (for per-player preview mode)
     */
    public PaperPreviewRenderer(JavaPlugin plugin, PlayerSessionStore sessions) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.sessions = Objects.requireNonNull(sessions, "sessions");
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

    /**
     * Plans a bounded outline for {@code selection}. Exposed for unit tests without a server.
     */
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
        PreviewMode mode = sessions.session(actor).previewMode();
        List<UUID> ids = new ArrayList<>();
        boolean usedParticles = false;

        for (BlockPosition point : points) {
            Location location = new Location(world, point.x(), point.y(), point.z());
            try {
                Entity entity = spawnPreview(world, location, mode, player);
                if (entity != null) {
                    player.showEntity(plugin, entity);
                    ids.add(entity.getUniqueId());
                }
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

    private Entity spawnPreview(World world, Location location, PreviewMode mode, Player player) {
        return switch (mode) {
            case BLOCK_LIGHT_BLUE -> spawnBlockDisplay(world, location, "minecraft:light_blue_stained_glass");
            case BLOCK_TINTED -> spawnBlockDisplay(world, location, "minecraft:tinted_glass");
            case BLOCK_CLEAR -> spawnBlockDisplay(world, location, "minecraft:glass");
            case TEXT_LOW -> spawnTextDisplay(world, location, Color.fromARGB(64, 0, 170, 255));
            case TEXT_HIGH -> spawnTextDisplay(world, location, Color.fromARGB(180, 0, 170, 255));
            case ITEM -> spawnItemDisplay(world, location);
            case ARMOR -> spawnArmorStand(world, location);
            case PARTICLE -> {
                spawnParticle(player, new Location(world, location.getX() + 0.5, location.getY() + 0.5, location.getZ() + 0.5));
                yield null;
            }
        };
    }

    private static BlockDisplay spawnBlockDisplay(World world, Location location, String block) {
        return world.spawn(location, BlockDisplay.class, entity -> {
            entity.setBlock(entity.getServer().createBlockData(block));
            entity.setPersistent(false);
            entity.setTransformation(new Transformation(
                    new Vector3f(0.05f, 0.05f, 0.05f),
                    new AxisAngle4f(),
                    new Vector3f(0.9f, 0.9f, 0.9f),
                    new AxisAngle4f()));
            entity.setVisibleByDefault(false);
        });
    }

    private static TextDisplay spawnTextDisplay(World world, Location location, Color color) {
        return world.spawn(location, TextDisplay.class, entity -> {
            entity.text(Component.empty());
            entity.setDefaultBackground(false);
            entity.setBackgroundColor(color);
            entity.setTextOpacity((byte) 0);
            entity.setSeeThrough(true);
            entity.setBillboard(Display.Billboard.CENTER);
            entity.setDisplayWidth(0.9f);
            entity.setDisplayHeight(0.9f);
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
        });
    }

    private static ItemDisplay spawnItemDisplay(World world, Location location) {
        return world.spawn(location, ItemDisplay.class, entity -> {
            entity.setItemStack(new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
            entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.HEAD);
            entity.setDisplayWidth(0.8f);
            entity.setDisplayHeight(0.8f);
            entity.setPersistent(false);
            entity.setVisibleByDefault(false);
        });
    }

    private static ArmorStand spawnArmorStand(World world, Location location) {
        return world.spawn(location.clone().add(0, -0.5, 0), ArmorStand.class, stand -> {
            stand.setVisible(false);
            stand.setMarker(true);
            stand.setSmall(true);
            stand.setPersistent(false);
            stand.setInvulnerable(true);
            stand.setGravity(false);
            stand.setBasePlate(false);
            stand.setArms(false);
            stand.setItem(EquipmentSlot.HEAD, new ItemStack(Material.LIGHT_BLUE_STAINED_GLASS));
        });
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
