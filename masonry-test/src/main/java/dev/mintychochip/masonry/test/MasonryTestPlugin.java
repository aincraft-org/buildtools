package dev.mintychochip.masonry.test;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class MasonryTestPlugin extends JavaPlugin {

    @Override
    public void onEnable() {
        getLogger().info("MasonryTest enabled");

        Plugin masonry = getServer().getPluginManager().getPlugin("Masonry");
        if (masonry == null) {
            getLogger().warning("Masonry not found; smoke test failed");
        } else if (!masonry.isEnabled()) {
            getLogger().warning("Masonry is present but not enabled");
        } else {
            getLogger().info("Masonry " + masonry.getDescription().getVersion() + " is present and enabled");
        }
    }

    @Override
    public void onDisable() {
        getLogger().info("MasonryTest disabled");
    }
}
