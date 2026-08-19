package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.tool.Tool;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Name-indexed tool catalog. Names must be unique and non-blank.
 */
public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

    /**
     * @param tool tool to register
     * @throws IllegalArgumentException if the name is blank
     * @throws IllegalStateException if the name is already registered
     */
    public void register(Tool tool) {
        Objects.requireNonNull(tool, "tool");
        String name = tool.name();
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Tool name must be present");
        }
        if (tools.putIfAbsent(name, tool) != null) {
            throw new IllegalStateException("Tool already registered: " + name);
        }
    }

    /** @param name tool name @return tool if registered */
    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    /**
     * @param name tool name
     * @return registered tool
     * @throws IllegalArgumentException if unknown
     */
    public Tool require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
    }

    /** @return snapshot of registered names */
    public Set<String> names() {
        return Set.copyOf(tools.keySet());
    }
}
