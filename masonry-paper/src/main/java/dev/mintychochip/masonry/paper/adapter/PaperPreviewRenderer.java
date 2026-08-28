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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import org.bukkit.Chunk;
import java.util.function.Function;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Team;

/**
 * Multi-mode preview renderer. Block and ghost previews use per-player fake block packets via
 * {@link Player#sendBlockChange(Location, BlockData)}; they never create server-side entities or
 * mutate world state. Text and experimental modes retain their explicit entity-based behavior.
 */
public final class PaperPreviewRenderer implements PreviewRenderer {
    static final int MAX_DISPLAYS = PreviewGeometry.DEFAULT_MAX_DISPLAYS;
    static final int MAX_SURFACE_BLOCKS = PreviewGeometry.DEFAULT_MAX_SURFACE_BLOCKS;
    private final JavaPlugin plugin;
    private final Server server;
    private final PlayerSessionStore sessions;
    private final Map<ActorId, Map<BlockPosition, UUID>> spawnedEntities = new HashMap<>();
    private final Map<ActorId, FakeBlockTracker<BlockData>> fakeBlocks = new HashMap<>();
    private final Map<ActorId, PreviewMode> shownModes = new HashMap<>();
    private final Team previewTeam;

    public PaperPreviewRenderer(JavaPlugin plugin, PlayerSessionStore sessions, Team previewTeam) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.server = plugin.getServer();
        this.sessions = Objects.requireNonNull(sessions, "sessions");
        this.previewTeam = Objects.requireNonNull(previewTeam, "previewTeam");
    }

    /**
     * Plans a complete bounded surface for {@code selection}. Oversized surfaces fall back to
     * the sparse outline cap rather than causing an unbounded packet burst.
     */
    public static List<BlockPosition> plan(CuboidSelection selection) {
        try {
            return PreviewGeometry.surface(selection, MAX_SURFACE_BLOCKS);
        } catch (IllegalArgumentException oversized) {
            return PreviewGeometry.outline(selection, MAX_DISPLAYS);
        }
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
        Map<BlockPosition, BlockData> wanted = new HashMap<>();
        for (Map.Entry<BlockOffset, BlockState> entry : clipboard.blocks().entrySet()) {
            BlockState state = entry.getValue();
            if (state == null || state.isAir()) {
                continue;
            }
            if (wanted.size() >= MAX_DISPLAYS) {
                break;
            }
            BlockOffset offset = entry.getKey();
            BlockPosition point = origin.offset(offset.x(), offset.y(), offset.z());
            try {
                wanted.put(point, server.createBlockData(PaperBlockStates.toBukkitString(state)));
            } catch (RuntimeException ignored) {
                // skip an invalid block state without aborting the rest of the ghost
            }
        }
        showFakeBlocks(actor, player, world, PreviewMode.BLOCK_CLEAR, wanted);
    }

    @Override
    public void clear(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        reset(actor);
    }
    /**
     * Re-sends every active fake block to its owning player after a chunk refresh or other client
     * resynchronization.
     *
     * @param actor viewer
     */
    public void resend(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        resendTracked(actor, position -> true);
    }

    /**
     * Re-sends only fake blocks contained in {@code chunk}.
     *
     * @param actor viewer
     * @param chunk refreshed chunk
     */
    public void resendChunk(ActorId actor, Chunk chunk) {
        Objects.requireNonNull(actor, "actor");
        Objects.requireNonNull(chunk, "chunk");
        String worldId = chunk.getWorld().getName();
        resendTracked(
                actor,
                position -> position.worldId().equals(worldId)
                        && position.x() >> 4 == chunk.getX()
                        && position.z() >> 4 == chunk.getZ());
    }

    private void resendTracked(ActorId actor, Predicate<BlockPosition> include) {
        Player player = server.getPlayer(actor.value());
        FakeBlockTracker<BlockData> tracker = fakeBlocks.get(actor);
        if (player == null || tracker == null) {
            return;
        }
        tracker.resend(include, (position, data) -> sendFakeBlock(player, position, data));
    }

    /**
     * Removes tracked entity previews, restores tracked fake blocks, and drops the viewer state.
     *
     * @param actor viewer
     */
    private void reset(ActorId actor) {
        Map<BlockPosition, UUID> previous = spawnedEntities.remove(actor);
        FakeBlockTracker<BlockData> fake = fakeBlocks.remove(actor);
        shownModes.remove(actor);
        if (fake != null) {
            Player player = server.getPlayer(actor.value());
            if (player != null) {
                fake.clear((position, original) -> restoreFakeBlock(player, position, original));
            }
        }
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
        if (shownModes.get(actor) != mode) {
            reset(actor);
            return new HashMap<>();
        }
        Map<BlockPosition, UUID> previous = spawnedEntities.get(actor);
        if (previous == null) {
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

    /**
     * Applies fake block states only to {@code player}. The server world is never changed and
     * no entity is created or ticked. Original block data is retained so every fake position can
     * be restored when the preview changes, clears, or the player logs out.
     */
    private void showFakeBlocks(
            ActorId actor,
            Player player,
            World world,
            PreviewMode mode,
            Map<BlockPosition, BlockData> wanted) {
        if (shownModes.get(actor) != mode) {
            reset(actor);
        }
        FakeBlockTracker<BlockData> tracker =
                fakeBlocks.computeIfAbsent(actor, ignored -> new FakeBlockTracker<>());
        tracker.show(
                wanted,
                position -> world.getBlockAt(position.x(), position.y(), position.z()).getBlockData().clone(),
                (position, data) -> sendFakeBlock(player, position, data),
                (position, original) -> restoreFakeBlock(player, position, original));
        if (tracker.isEmpty()) {
            fakeBlocks.remove(actor);
        }
        shownModes.put(actor, mode);
    }

    private void restoreFakeBlock(Player player, BlockPosition position, BlockData original) {
        World world = server.getWorld(position.worldId());
        if (world == null) {
            return;
        }
        BlockData restore = original;
        try {
            restore = world.getBlockAt(position.x(), position.y(), position.z()).getBlockData();
        } catch (RuntimeException ignored) {
            // use the captured state when the live block cannot be read
        }
        try {
            player.sendBlockChange(new Location(world, position.x(), position.y(), position.z()), restore);
        } catch (RuntimeException ignored) {
            // the viewer may be leaving or the chunk may have unloaded
        }
    }
    private void sendFakeBlock(Player player, BlockPosition position, BlockData data) {
        World world = server.getWorld(position.worldId());
        if (world != null) {
            player.sendBlockChange(new Location(world, position.x(), position.y(), position.z()), data);
        }
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
            default -> showFakeBlockPreview(actor, region, world, player, mode);
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
        spawnedEntities.put(actor, ids);
        shownModes.put(actor, mode);
    }
    private void showFakeBlockPreview(
            ActorId actor, CuboidSelection region, World world, Player player, PreviewMode mode) {
        String block = switch (mode) {
            case BLOCK_TINTED -> "minecraft:tinted_glass";
            case BLOCK_CLEAR -> "minecraft:glass";
            default -> "minecraft:light_blue_stained_glass";
        };
        BlockData data = server.createBlockData(block);
        Map<BlockPosition, BlockData> wanted = new HashMap<>();
        for (BlockPosition point : plan(region)) {
            wanted.put(point, data);
        }
        showFakeBlocks(actor, player, world, mode, wanted);
    }

    private void showParticles(ActorId actor, CuboidSelection region, World world, Player player) {
        List<BlockPosition> points = plan(region);
        Map<BlockPosition, UUID> next = retain(actor, PreviewMode.PARTICLE, List.of());
        for (BlockPosition point : points) {
            spawnParticle(player, new Location(world, point.x() + 0.5, point.y() + 0.5, point.z() + 0.5));
        }
        spawnedEntities.put(actor, next);
        shownModes.put(actor, PreviewMode.PARTICLE);
    }


    /**
     * Renders the selection as client-side fake shulker-box blocks along the outline edges.
     * Unlike a server entity, these blocks are sent only to {@code player}.
     */
    private void showShulkerOutline(ActorId actor, CuboidSelection region, World world, Player player) {
        BlockData shulkerData = server.createBlockData("minecraft:white_shulker_box");
        Map<BlockPosition, BlockData> wanted = new HashMap<>();
        for (BlockPosition point : plan(region)) {
            wanted.put(point, shulkerData);
        }
        showFakeBlocks(actor, player, world, PreviewMode.SHULKER, wanted);
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
        spawnedEntities.put(actor, next);
        shownModes.put(actor, mode);
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

    private static BlockPosition keyOf(Location location) {
        return new BlockPosition(
                location.getWorld().getName(),
                location.getBlockX(),
                location.getBlockY(),
                location.getBlockZ());
    }

    static final class FakeBlockTracker<T> {
        private final Map<BlockPosition, T> originals = new HashMap<>();
        private final Map<BlockPosition, T> shown = new HashMap<>();

        void show(
                Map<BlockPosition, T> wanted,
                Function<BlockPosition, T> originalAt,
                BiConsumer<BlockPosition, T> send,
                BiConsumer<BlockPosition, T> restore) {
            var stale = originals.entrySet().iterator();
            while (stale.hasNext()) {
                Map.Entry<BlockPosition, T> entry = stale.next();
                if (!wanted.containsKey(entry.getKey())) {
                    restore.accept(entry.getKey(), entry.getValue());
                    shown.remove(entry.getKey());
                    stale.remove();
                }
            }
            for (Map.Entry<BlockPosition, T> entry : wanted.entrySet()) {
                BlockPosition position = entry.getKey();
                boolean newlyTracked = false;
                try {
                    if (!originals.containsKey(position)) {
                        originals.put(position, Objects.requireNonNull(originalAt.apply(position), "original"));
                        newlyTracked = true;
                    }
                    send.accept(position, entry.getValue());
                    shown.put(position, entry.getValue());
                } catch (RuntimeException ignored) {
                    if (newlyTracked) {
                        originals.remove(position);
                        shown.remove(position);
                    }
                }
            }
        }

        void resend(Predicate<BlockPosition> include, BiConsumer<BlockPosition, T> send) {
            for (Map.Entry<BlockPosition, T> entry : shown.entrySet()) {
                if (include.test(entry.getKey())) {
                    try {
                        send.accept(entry.getKey(), entry.getValue());
                    } catch (RuntimeException ignored) {
                        // the viewer may be leaving or the chunk may be unavailable
                    }
                }
            }
        }

        void clear(BiConsumer<BlockPosition, T> restore) {
            try {
                originals.forEach(restore);
            } finally {
                originals.clear();
                shown.clear();
            }
        }

        boolean isEmpty() {
            return originals.isEmpty();
        }
    }

    private record Face(Location location, float width, float height) {}

    private enum Experimental { ITEM, ARMOR }
}
