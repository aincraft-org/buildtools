package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.selection.CuboidSelection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record ToolRequest(
        ActorId actorId,
        String toolName,
        CuboidSelection selection,
        Map<String, String> arguments,
        Clipboard clipboard) {
    public ToolRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(toolName, "toolName");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must be present");
        }
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
    }

    public ToolRequest(ActorId actorId, String toolName, CuboidSelection selection, Map<String, String> arguments) {
        this(actorId, toolName, selection, arguments, null);
    }

    public Optional<CuboidSelection> selectionOptional() {
        return Optional.ofNullable(selection);
    }

    public Optional<Clipboard> clipboardOptional() {
        return Optional.ofNullable(clipboard);
    }

    public String argument(String key) {
        return arguments.get(key);
    }
}
