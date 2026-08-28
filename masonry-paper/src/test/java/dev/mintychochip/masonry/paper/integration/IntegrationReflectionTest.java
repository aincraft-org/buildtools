package dev.mintychochip.masonry.paper.integration;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import dev.mintychochip.masonry.api.ActorId;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.UUID;
import org.bukkit.block.Block;
import org.junit.jupiter.api.Test;

/**
 * Verifies the reflective WorldGuard method lookups used by the integration resolve against
 * the real plugin jar. The fixture lives outside the repo; the test is skipped when absent so
 * clean checkouts stay green. (FAWE class loading needs the full plugin runtime classpath,
 * which only exists on a live server, so it is covered by the server smoke instead.)
 */
class IntegrationReflectionTest {

    private static final File WG_BUKKIT = new File("/tmp/wg-bukkit-7.0.14.jar");

    @Test
    void worldGuardReflectionResolvesAgainstRealJars() throws Exception {
        assumeTrue(WG_BUKKIT.isFile(), "WorldGuard fixture jar not present; skipping");
        ClassLoader loader = new URLClassLoader(
                new URL[] {WG_BUKKIT.toURI().toURL()},
                getClass().getClassLoader());
        Class<?> protectionQuery =
                Class.forName("com.sk89q.worldguard.bukkit.ProtectionQuery", true, loader);
        Object query = protectionQuery.getConstructor().newInstance();
        assertNotNull(query);
        assertNotNull(protectionQuery.getMethod("testBlockBreak", Object.class, Block.class));
        assertNotNull(protectionQuery.getMethod(
                "testBlockPlace", Object.class, org.bukkit.Location.class, org.bukkit.Material.class));
    }

    @Test
    void regionGuardNoneAllowsEverything() {
        RegionGuard guard = RegionGuard.NONE;
        assertTrue(guard.canBreak(new ActorId(UUID.randomUUID()), null));
        assertTrue(guard.canPlace(new ActorId(UUID.randomUUID()), null, null));
    }
}