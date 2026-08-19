package dev.mintychochip.buildtools.common.tool;

import dev.mintychochip.buildtools.api.tool.Tool;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public final class ToolRegistry {
    private final Map<String, Tool> tools = new LinkedHashMap<>();

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

    public Optional<Tool> find(String name) {
        return Optional.ofNullable(tools.get(name));
    }

    public Tool require(String name) {
        return find(name).orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + name));
    }

    public Set<String> names() {
        return Set.copyOf(tools.keySet());
    }
}
