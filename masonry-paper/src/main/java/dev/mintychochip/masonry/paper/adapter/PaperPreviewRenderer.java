package dev.mintychochip.masonry.paper.adapter;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.BlockOffset;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.preview.PreviewMode;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import dev.mintychochip.masonry.api.service.PreviewRenderer;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.common.preview.PreviewGeometry;
import dev.mintychochip.masonry.common.session.PlayerSessionStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import java.util.concurrent.atomic.AtomicReference;
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
import org.bukkit.entity.Shulker;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import org.bukkit.scoreboard.Team;

/**
 * Multi-mode preview renderer. Viable modes are translucent-glass {@link BlockDisplay},
 * six-face alpha {@link TextDisplay}, and particle outline. {@code EXPERIMENTAL_*} modes
 * are fixed-model demos that may not scale or handle true alpha as well.
 */
public final class PaperPreviewRenderer implements PreviewRenderer {
    static final int MAX_DISPLAYS = PreviewGeometry.DEFAULT_MAX_DISPLAYS;

    private final JavaPlugin plugin;
    private final Server server;
    private final PlayerSessionStore sessions;
    private final Map<ActorId, Map<BlockPosition, UUID>> spawned = new HashMap<>();
    private final Map<ActorId, PreviewMode> shownModes = new HashMap<>();
    private final Team previewTeam;

    public PaperPreviewRenderer(JavaPlugin plugin, PlayerSessionStore sessions, Team previewTeam) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.previewTeam = Objects.requireNonNull(previewTeam, "previewTeam");
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
    public void showGhost(ActorId actor, Clipboard clipboard, BlockPosition origin) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(clipboard, "clipboard");
        Objects.requireNonNull(origin, "origin");
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            reset(actor);
            return;
        }
        World world = server.getWorld(origin.worldId());
        if (world == null) {
            reset(actor);
            return;
        }
        List<BlockPosition> points = clipboard.blocks().keySet().stream()
                .map(offset -> origin.offset(offset.x(), offset.y(), offset.z()))
                .filter(position -> {
                    BlockState state = clipboard.blocks().get(
                            new BlockOffset(
                                    position.x() - origin.x(),
                                    position.y() - origin.y(),
                                    position.z() - origin.z()));
                    return state != null && !state.isAir();
                })
                .toList();
        if (points.size() > MAX_DISPLAYS) {
            points = points.subList(0, MAX_DISPLAYS);
        }
        Map<BlockPosition, UUID> next = retain(actor, PreviewMode.BLOCK_CLEAR, points);
        for (BlockPosition point : points) {
            if (next.containsKey(point)) {
                continue;
            }
            BlockState state = clipboard.blocks().get(
                    new BlockOffset(
                            point.x() - origin.x(),
                            point.y() - origin.y(),
                            point.z() - origin.z()));
            if (state == null || state.isAir()) {
                continue;
            }
            Location location = new Location(world, point.x(), point.y(), point.z());
            try {
                BlockData data = server.createBlockData(PaperBlockStates.toBukkitString(state));
                BlockDisplay display = spawnAnimatedBlockDisplay(
                        world, player, location, origin, data, new Transformation(
                                new Vector3f(0f, 0f, 0f),
                                new AxisAngle4f(),
                                new Vector3f(1f, 1f, 1f),
                                new AxisAngle4f()));
                next.put(point, display.getUniqueId());
            } catch (RuntimeException ignored) {
                // skip this cell
            }
        }
        spawned.put(actor, next);
        shownModes.put(actor, PreviewMode.BLOCK_CLEAR);
    }

    @Override
    public void clear(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        reset(actor);
    }

    /**
     * Removes every tracked entity for {@code actor} and drops its tracking state.
     *
     * @param actor viewer
     */
    private void reset(ActorId actor) {
        Map<BlockPosition, UUID> previous = spawned.remove(actor);
        shownModes.remove(actor);
        if (previous == null) {
            return;
        }
        for (UUID id : previous.values()) {
            Entity entity = server.getEntity(id);
            if (entity != null) {
                previewTeam.removeEntity(entity);
                entity.remove();
            }
        }
    }

    /**
     * Returns the surviving position→uuid map after removing entities that are no longer
     * wanted. When the preview mode changed, every previous entity is removed first.
     *
     * @param actor viewer
     * @param mode mode being rendered
     * @param wanted positions that must survive
     * @return map of wanted positions that already had a display
     */
    private Map<BlockPosition, UUID> retain(ActorId actor, PreviewMode mode, List<BlockPosition> wanted) {
        Map<BlockPosition, UUID> previous = spawned.get(actor);
        if (previous == null) {
            return new HashMap<>();
        }
        if (shownModes.get(actor) != mode) {
            reset(actor);
            return new HashMap<>();
        }
        Map<BlockPosition, UUID> next = new HashMap<>();
        for (BlockPosition position : wanted) {
            UUID existing = previous.get(position);
            if (existing != null && server.getEntity(existing) != null) {
                next.put(position, existing);
            }
        }
        for (Map.Entry<BlockPosition, UUID> entry : previous.entrySet()) {
            if (!next.containsKey(entry.getKey())) {
                Entity entity = server.getEntity(entry.getValue());
                if (entity != null) {
                    previewTeam.removeEntity(entity);
                    entity.remove();
                }
            }
        }
        return next;
    }

    private void render(ActorId actor, CuboidSelection region) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            reset(actor);
            return;
        }
        World world = server.getWorld(region.worldId());
        if (world == null) {
            reset(actor);
            return;
        }
        PreviewMode mode = sessions.session(actor).previewMode();
        switch (mode) {
            case TEXT_LOW -> showTextCube(actor, region, world, player, Color.fromARGB(64, 0, 170, 255), mode);
            case TEXT_HIGH -> showTextCube(actor, region, world, player, Color.fromARGB(180, 0, 170, 255), mode);
            case PARTICLE -> showParticles(actor, region, world, player);
            case SHULKER -> showShulkerOutline(actor, region, world, player);
            case EXPERIMENTAL_ITEM -> showExperimental(actor, region, world, player, Experimental.ITEM);
            case EXPERIMENTAL_ARMOR -> showExperimental(actor, region, world, player, Experimental.ARMOR);
            default -> showBlockDisplays(actor, region, world, player, mode);
        }
    }

    private void showTextCube(
            ActorId actor, CuboidSelection region, World world, Player player, Color color, PreviewMode mode) {
        reset(actor);
        BlockPosition min = region.min();
        BlockPosition max = region.max();
        Map<BlockPosition, UUID> ids = new HashMap<>();

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
                ids.put(keyOf(face.location()), display.getUniqueId());
            } catch (RuntimeException ignored) {
                // fall through to no preview
            }
        }
        spawned.put(actor, ids);
        shownModes.put(actor, mode);
    }

    private void showBlockDisplays(ActorId actor, CuboidSelection region, World world, Player player, PreviewMode mode) {
        String block = switch (mode) {
            case BLOCK_TINTED -> "minecraft:tinted_glass";
            case BLOCK_CLEAR -> "minecraft:glass";
            default -> "minecraft:light_blue_stained_glass";
        };
        BlockData data = server.createBlockData(block);
        List<BlockPosition> points = plan(region);
        Map<BlockPosition, UUID> next = retain(actor, mode, points);

        BlockPosition source = region.min();
        for (BlockPosition point : points) {
            if (next.containsKey(point)) {
                continue;
            }
            Location location = new Location(world, point.x(), point.y(), point.z());
            try {
                BlockDisplay display = spawnAnimatedBlockDisplay(
                        world, player, location, source, data, new Transformation(
                                new Vector3f(0.05f, 0.05f, 0.05f),
                                new AxisAngle4f(),
                                new Vector3f(0.9f, 0.9f, 0.9f),
                                new AxisAngle4f()));
                next.put(point, display.getUniqueId());
            } catch (RuntimeException ignored) {
                // fall back to particle for this point
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        spawned.put(actor, next);
        shownModes.put(actor, mode);
    }

    private void showParticles(ActorId actor, CuboidSelection region, World world, Player player) {
        List<BlockPosition> points = plan(region);
        Map<BlockPosition, UUID> next = retain(actor, PreviewMode.PARTICLE, List.of());
        for (BlockPosition point : points) {
            spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
        }
        spawned.put(actor, next);
        shownModes.put(actor, PreviewMode.PARTICLE);
    }

    /**
     * Renders the selection as client-side invisible, glowing shulker boxes along the outline
     * edges. Each shulker is spawned invisible to everyone and shown only to {@code player},
     * with a glow color that traces the region boundary.
     */
    private void showShulkerOutline(ActorId actor, CuboidSelection region, World world, Player player) {
        List<BlockPosition> points = plan(region);
        Map<BlockPosition, UUID> next = retain(actor, PreviewMode.SHULKER, points);

        for (BlockPosition point : points) {
            if (next.containsKey(point)) {
                continue;
            }
            // world.spawn places the entity by its feet; a shulker is 1.0 tall, so its base
            // must rest at point.y() for it to fill the block cell (x/z center on the block).
            Location location = new Location(world, point.x() + 0.5, point.y(), point.z() + 0.5);
            try {
                Shulker shulker = world.spawn(location, Shulker.class, entity -> {
                    entity.setInvisible(true);
                    entity.setGlowing(true);
                    entity.setSilent(true);
                    entity.setAI(false);
                    entity.setNoPhysics(true);
                    entity.setCollidable(false);
                    entity.setInvulnerable(true);
                    entity.setGravity(false);
                    entity.setPersistent(false);
                    entity.setVisibleByDefault(false);
                });
                previewTeam.addEntity(shulker);
                player.showEntity(plugin, shulker);
                next.put(point, shulker.getUniqueId());
            } catch (RuntimeException ignored) {
                // fall back to particle for this point
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        spawned.put(actor, next);
        shownModes.put(actor, PreviewMode.SHULKER);
    }

    private void showExperimental(ActorId actor, CuboidSelection region, World world, Player player, Experimental kind) {
        PreviewMode mode = switch (kind) {
            case ITEM -> PreviewMode.EXPERIMENTAL_ITEM;
            case ARMOR -> PreviewMode.EXPERIMENTAL_ARMOR;
        };
        List<BlockPosition> points = plan(region);
        Map<BlockPosition, UUID> next = retain(actor, mode, points);

        for (BlockPosition point : points) {
            if (next.containsKey(point)) {
                continue;
            }
            Location location = new Location(world, point.x(), point.y(), point.z());
            try {
                Entity entity = switch (kind) {
                    case ITEM -> spawnItemDisplay(world, location);
                    case ARMOR -> spawnArmorStand(world, location);
                };
                player.showEntity(plugin, entity);
                next.put(point, entity.getUniqueId());
            } catch (RuntimeException ignored) {
                // fall back to particle for this point
                spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
            }
        }
        spawned.put(actor, next);
        shownModes.put(actor, mode);
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
    private BlockDisplay spawnAnimatedBlockDisplay(
            World world,
            Player player,
            Location target,
            BlockPosition source,
            BlockData data,
            Transformation transform) {
        AtomicReference<Transformation> finalTransform = new AtomicReference<>();
        BlockDisplay entity = world.spawn(target, BlockDisplay.class, e -> {
            e.setBlock(data);
            e.setPersistent(false);
            e.setInterpolationDuration(10);
            e.setInterpolationDelay(0);
            e.setVisibleByDefault(false);
            e.setTransformation(transform);
            finalTransform.set(e.getTransformation());
            e.setTransformation(translate(finalTransform.get(), source, target));
        });
        player.showEntity(plugin, entity);
        server.getScheduler().runTaskLater(plugin, () -> {
            if (entity.isValid()) {
                entity.setTransformation(finalTransform.get());
            }
        }, 1L);
        return entity;
    }

    private static Transformation translate(Transformation t, BlockPosition source, Location target) {
        Vector3f offset = new Vector3f(
                source.x() - (float) target.getX(),
                source.y() - (float) target.getY(),
                source.z() - (float) target.getZ());
        return new Transformation(
                new Vector3f(t.getTranslation()).add(offset),
                t.getLeftRotation(),
                t.getScale(),
                t.getRightRotation());
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

    private static BlockPosition keyOf(Location location) {
        return new BlockPosition(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    private record Face(Location location, float width, float height) {}

    private enum Experimental { ITEM, ARMOR }
}
