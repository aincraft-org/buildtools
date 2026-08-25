package dev.mintychochip.buildtools.paper.adapter;

import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * ItemStack factory and detector for the BuildTools gadget.
 */
public final class GadgetItem {
    private static final String GADGET_KEY = "buildtools_gadget";

    private GadgetItem() {}

    /**
     * @param plugin owning plugin
     * @return an unbreakable blaze rod tagged as the BuildTools gadget
     */
    public static ItemStack create(JavaPlugin plugin) {
        ItemStack item = new ItemStack(Material.BLAZE_ROD);
        item.editMeta(meta -> {
            meta.displayName(Component.text("BuildTools Gadget", NamedTextColor.AQUA));
            meta.lore(List.of(
                    Component.text("Shift-left: cycle mode", NamedTextColor.GRAY),
                    Component.text("Shift-right: set pos1", NamedTextColor.GRAY),
                    Component.text("Right-click: set pos2 / apply", NamedTextColor.GRAY),
                    Component.text("Swap-hands: undo · Sneak+swap: redo", NamedTextColor.GRAY),
                    Component.text("Aim: live box preview", NamedTextColor.GRAY)));
            meta.setUnbreakable(true);
            meta.getPersistentDataContainer().set(
                    new NamespacedKey(plugin, GADGET_KEY),
                    PersistentDataType.BYTE, (byte) 1);
        });
        return item;
    }

    /**
     * @param plugin owning plugin
     * @param item item to check
     * @return {@code true} if the item is the BuildTools gadget
     */
    public static boolean isGadget(JavaPlugin plugin, ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(
                new NamespacedKey(plugin, GADGET_KEY),
                PersistentDataType.BYTE);
    }
}
