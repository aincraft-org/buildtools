package dev.mintychochip.masonry.paper.integration;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.operation.BlockChange;
import dev.mintychochip.masonry.api.world.BlockPosition;
import dev.mintychochip.masonry.api.world.BlockState;
import dev.mintychochip.masonry.paper.adapter.PaperBlockStates;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Committed FastAsyncWorldEdit batch writer. All FAWE access is reflective through the FAWE
 * plugin's class loader, so Masonry enables without FAWE and simply falls back to Bukkit.
 *
 * <p>Callers are expected to have preflighted Bukkit events and region protection
 * synchronously; this class only applies the committed diff set and flushes it.
 */
final class FaweBatchWriter {
    private final Logger logger;
    private final Server server;
    private final Object editSessionFactory;
    private final Method adaptWorld;
    private final Method adaptPlayer;
    private final Method adaptBlockData;
    private final Method newEditSession;
    private final Method newEditSessionUntracked;
    private final Method setBlock;
    private final Method setBlockChangeLimit;
    private final Method setFastMode;
    private final Method close;

    private FaweBatchWriter(
            Logger logger,
            Server server,
            Object editSessionFactory,
            Method adaptWorld,
            Method adaptPlayer,
            Method adaptBlockData,
            Method newEditSession,
            Method newEditSessionUntracked,
            Method setBlock,
            Method setBlockChangeLimit,
            Method setFastMode,
            Method close) {
        this.logger = logger;
        this.server = server;
        this.editSessionFactory = editSessionFactory;
        this.adaptWorld = adaptWorld;
        this.adaptPlayer = adaptPlayer;
        this.adaptBlockData = adaptBlockData;
        this.newEditSession = newEditSession;
        this.newEditSessionUntracked = newEditSessionUntracked;
        this.setBlock = setBlock;
        this.setBlockChangeLimit = setBlockChangeLimit;
        this.setFastMode = setFastMode;
        this.close = close;
    }

    /**
     * Attempts to build a FAWE writer.
     *
     * @param logger plugin logger
     * @param server running server
     * @return writer, or {@code null} when FAWE is not usable
     */
    static FaweBatchWriter resolve(Logger logger, Server server) {
        Plugin fawe = server.getPluginManager().getPlugin("FastAsyncWorldEdit");
        if (fawe == null) {
            fawe = server.getPluginManager().getPlugin("WorldEdit");
        }
        if (fawe == null) {
            return null;
        }
        ClassLoader loader = fawe.getClass().getClassLoader();
        try {
            Class.forName("com.fastasyncworldedit.core.FaweAPI", true, loader);
            Class<?> worldEditClass = Class.forName("com.sk89q.worldedit.WorldEdit", true, loader);
            Method getInstance = worldEditClass.getMethod("getInstance");
            Object worldEdit = getInstance.invoke(null);
            Method getEditSessionFactory = worldEditClass.getMethod("getEditSessionFactory");
            Object factory = getEditSessionFactory.invoke(worldEdit);

            Class<?> bukkitAdapter = Class.forName("com.sk89q.worldedit.bukkit.BukkitAdapter", true, loader);
            Method adaptWorldMethod = bukkitAdapter.getMethod("adapt", org.bukkit.World.class);
            Method adaptPlayerMethod = bukkitAdapter.getMethod("adapt", Player.class);
            Method adaptBlockDataMethod =
                    bukkitAdapter.getMethod("adapt", org.bukkit.block.data.BlockData.class);

            Class<?> worldClass = Class.forName("com.sk89q.worldedit.world.World", true, loader);
            Class<?> actorClass = Class.forName("com.sk89q.worldedit.extension.platform.Actor", true, loader);
            Class<?> editSessionClass = Class.forName("com.sk89q.worldedit.EditSession", true, loader);
            Method newSession = factory.getClass().getMethod("getEditSession", worldClass, int.class, actorClass);
            Method newSessionUntracked = factory.getClass().getMethod("getEditSession", worldClass, int.class);

            Class<?> patternClass =
                    Class.forName("com.sk89q.worldedit.function.pattern.Pattern", true, loader);
            Method setBlockMethod = editSessionClass.getMethod(
                    "setBlock", int.class, int.class, int.class, patternClass);
            Method setLimit = editSessionClass.getMethod("setBlockChangeLimit", long.class);
            Method fastMode = editSessionClass.getMethod("setFastMode", boolean.class);
            Method closeMethod = editSessionClass.getMethod("close");

            logger.info("Masonry: FastAsyncWorldEdit write offload enabled.");
            return new FaweBatchWriter(
                    logger,
                    server,
                    factory,
                    adaptWorldMethod,
                    adaptPlayerMethod,
                    adaptBlockDataMethod,
                    newSession,
                    newSessionUntracked,
                    setBlockMethod,
                    setLimit,
                    fastMode,
                    closeMethod);
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.log(Level.WARNING, "Masonry: WorldEdit detected but its API is incompatible; "
                    + "falling back to Bukkit writes.", e);
            return null;
        }
    }

    /**
     * Applies {@code changes} in one committed FAWE batch per world.
     *
     * @param actor acting player (may be offline)
     * @param changes ordered diff set, all preflighted
     * @return {@code false} if FAWE reported a failure (rollback is best-effort)
     */
    boolean write(ActorId actor, List<BlockChange> changes) {
        Map<String, List<BlockChange>> byWorld = new LinkedHashMap<>();
        for (BlockChange change : changes) {
            byWorld.computeIfAbsent(change.position().worldId(), ignored -> new ArrayList<>()).add(change);
        }
        for (Map.Entry<String, List<BlockChange>> entry : byWorld.entrySet()) {
            if (!writeWorld(actor, entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        return true;
    }

    private boolean writeWorld(ActorId actor, String worldId, List<BlockChange> changes) {
        org.bukkit.World bukkitWorld = server.getWorld(worldId);
        if (bukkitWorld == null) {
            throw new IllegalArgumentException("Unknown world: " + worldId);
        }
        Object session = null;
        List<BlockChange> queued = new ArrayList<>();
        try {
            Object faweWorld = adaptWorld.invoke(null, bukkitWorld);
            Player player = server.getPlayer(actor.value());
            if (player != null) {
                Object fawePlayer = adaptPlayer.invoke(null, player);
                session = newEditSession.invoke(editSessionFactory, faweWorld, changes.size(), fawePlayer);
            } else {
                session = newEditSessionUntracked.invoke(editSessionFactory, faweWorld, changes.size());
            }
            setBlockChangeLimit.invoke(session, (long) changes.size());
            setFastMode.invoke(session, true);
            for (BlockChange change : changes) {
                BlockState after = change.after();
                BlockData blockData =
                        server.createBlockData(PaperBlockStates.toBukkitString(after));
                Object faweState = adaptBlockData.invoke(null, blockData);
                BlockPosition position = change.position();
                setBlock.invoke(session, position.x(), position.y(), position.z(), faweState);
                queued.add(change);
            }
            close.invoke(session);
            session = null;
            return true;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            logger.log(Level.WARNING, "Masonry: FAWE batch failed for world " + worldId
                    + "; restoring " + queued.size() + " blocks.", e);
            // Drop the failed session unflushed so its queued target blocks are never
            // committed after the rollback, then restore the original states.
            if (session != null) {
                try {
                    close.invoke(session);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Session is already failing; nothing else to do.
                }
                session = null;
            }
            rollbackBestEffort(worldId, queued);
            return false;
        } finally {
            if (session != null) {
                try {
                    close.invoke(session);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Session is already failing; nothing else to do.
                }
            }
        }
    }

    /**
     * Best-effort restore of any queued-but-unflushed changes using the original {@code before}
     * states, so a FAWE failure does not silently leave the world half-written.
     */
    private void rollbackBestEffort(String worldId, List<BlockChange> queued) {
        if (queued.isEmpty()) {
            return;
        }
        org.bukkit.World bukkitWorld = server.getWorld(worldId);
        if (bukkitWorld == null) {
            return;
        }
        Object session = null;
        try {
            Object faweWorld = adaptWorld.invoke(null, bukkitWorld);
            session = newEditSessionUntracked.invoke(editSessionFactory, faweWorld, queued.size());
            setBlockChangeLimit.invoke(session, (long) queued.size());
            setFastMode.invoke(session, true);
            for (BlockChange change : queued) {
                BlockState before = change.before();
                BlockData blockData =
                        server.createBlockData(PaperBlockStates.toBukkitString(before));
                Object faweState = adaptBlockData.invoke(null, blockData);
                BlockPosition position = change.position();
                setBlock.invoke(session, position.x(), position.y(), position.z(), faweState);
            }
            close.invoke(session);
            session = null;
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            logger.log(Level.SEVERE, "Masonry: FAWE rollback failed for world " + worldId
                    + "; world may be partially modified.", e);
        } finally {
            if (session != null) {
                try {
                    close.invoke(session);
                } catch (ReflectiveOperationException | RuntimeException ignored) {
                    // Ignore.
                }
            }
        }
    }
}