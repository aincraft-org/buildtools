package dev.mintychochip.masonry.paper.adapter;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.junit.jupiter.api.Test;

class GadgetItemTest {
    @Test
    void onlyTheLiteralBrickMaterialActivatesExtensionMode() {
        assertTrue(GadgetItem.isExtensionToken(Material.BRICK));
        assertFalse(GadgetItem.isExtensionToken(Material.BRICKS));
        assertFalse(GadgetItem.isExtensionToken(Material.STONE));
        assertFalse(GadgetItem.isExtensionToken((Material) null));
    }

}
