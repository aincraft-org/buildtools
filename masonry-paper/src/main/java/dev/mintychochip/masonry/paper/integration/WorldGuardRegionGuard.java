package dev.mintychochip.masonry.paper.integration;

import dev.mintychochip.masonry.api.ActorId;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * {@link RegionGuard} backed by WorldGuard's {@code ProtectionQuery} via reflection.
 * The class has no compile-time WorldGuard dependency: it resolves WorldGuard classes
 * through the WorldGuard plugin's own class loader, so Masonry still enables with
 * WorldGuard absent.
 */
final class WorldGuardRegionGuard implements RegionGuard {
    private static final String WORLD_GUARD_PLUGIN = "WorldGuard";
    private final Logger logger;
    private final Server server;
    private final Object protectionQuery;
    private final Method testBlockBreak;
    private final Method testBlockPlace;

    private WorldGuardRegionGuard(Logger logger, Server server, Object protectionQuery,
                                  Method testBlockBreak, Method testBlockPlace) {
        this.logger = logger;
        this.server = server;
        this.protectionQuery = protectionQuery;
        this.testBlockBreak = testBlockBreak;
        this.testBlockPlace = testBlockPlace;
    }

    /**
     * Attempts to create a WorldGuard-backed guard.
     *
     * @param logger plugin logger
     * @param server running server
     * @return the guard, or {@link RegionGuard#NONE} when WorldGuard is not usable
     */
    static RegionGuard resolve(Logger logger, Server server) {
        Plugin worldGuard = server.getPluginManager().getPlugin(WORLD_GUARD_PLUGIN);
        if (worldGuard == null) {
            return RegionGuard.NONE;
        }
        try {
            ClassLoader loader = worldGuard.getClass().getClassLoader();
            Class<?> protectionQueryClass =
                    Class.forName("com.sk89q.worldguard.bukkit.ProtectionQuery", true, loader);
            Object query = protectionQueryClass.getConstructor().newInstance();
            Method breakMethod = protectionQueryClass.getMethod("testBlockBreak", Object.class, Block.class);
            Method placeMethod = protectionQueryClass.getMethod(
                    "testBlockPlace", Object.class, org.bukkit.Location.class, org.bukkit.Material.class);
            logger.info("Masonry: WorldGuard region protection enabled.");
            return new WorldGuardRegionGuard(logger, server, query, breakMethod, placeMethod);
        } catch (ReflectiveOperationException | LinkageError e) {
            logger.log(Level.WARNING, "Masonry: WorldGuard detected but its API is incompatible; "
                    + "region protection disabled.", e);
            return RegionGuard.NONE;
        }
    }

    @Override
    public boolean canBreak(ActorId actor, Block block) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return true;
        }
        try {
            return (boolean) testBlockBreak.invoke(protectionQuery, player, block);
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.WARNING, "Masonry: WorldGuard break check failed; refusing write.", e);
            return false;
        }
    }

    @Override
    public boolean canPlace(ActorId actor, Block block, BlockData newData) {
        Player player = server.getPlayer(actor.value());
        if (player == null) {
            return true;
        }
        try {
            return (boolean) testBlockPlace.invoke(protectionQuery, player, block.getLocation(), newData.getMaterial());
        } catch (IllegalAccessException | InvocationTargetException e) {
            logger.log(Level.WARNING, "Masonry: WorldGuard place check failed; refusing write.", e);
            return false;
        }
    }
}