package dev.mintychochip.masonry.common.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Tests register, resolve, duplicate names, and unknown lookups. */
class ToolRegistryTest {
    @Test
    void registersAndResolvesByName() {
        ToolRegistry registry = new ToolRegistry();
        FillTool tool = new FillTool();
        registry.register(tool);

        assertEquals(tool, registry.require("fill"));
        assertTrue(registry.find("fill").isPresent());
        assertTrue(registry.names().contains("fill"));
    }

    @Test
    void rejectsDuplicateNamesAndUnknownLookups() {
        ToolRegistry registry = new ToolRegistry();
        registry.register(new FillTool());

        assertThrows(IllegalStateException.class, () -> registry.register(new FillTool()));
        assertThrows(IllegalArgumentException.class, () -> registry.require("replace"));
    }
}
