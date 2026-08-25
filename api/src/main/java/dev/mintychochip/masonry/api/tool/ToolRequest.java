package dev.mintychochip.masonry.api.tool;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.selection.CuboidSelection;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Input to a tool invocation.
 *
 * @param actorId acting player
 * @param toolName registered name
 * @param selection region or paste origin; may be {@code null}
 * @param arguments tool-specific keys ({@code from}/{@code to}/{@code block})
 * @param clipboard paste source; may be {@code null}
 */
public record ToolRequest(
        ActorId actorId,
        String toolName,
        CuboidSelection selection,
        Map<String, String> arguments,
        Clipboard clipboard) {
    /**
     * @throws NullPointerException if actor or tool name is {@code null}
     * @throws IllegalArgumentException if tool name is blank
     */
    public ToolRequest {
        Objects.requireNonNull(actorId, "actorId");
        Objects.requireNonNull(toolName, "toolName");
        if (toolName.isBlank()) {
            throw new IllegalArgumentException("toolName must be present");
        }
        arguments = Map.copyOf(arguments == null ? Map.of() : arguments);
    }

    /**
     * Request without a clipboard.
     *
     * @param actorId actor
     * @param toolName tool
     * @param selection selection
     * @param arguments arguments
     */
    public ToolRequest(ActorId actorId, String toolName, CuboidSelection selection, Map<String, String> arguments) {
        this(actorId, toolName, selection, arguments, null);
    }

    /** @return selection if present */
    public Optional<CuboidSelection> selectionOptional() {
        return Optional.ofNullable(selection);
    }

    /** @return clipboard if present */
    public Optional<Clipboard> clipboardOptional() {
        return Optional.ofNullable(clipboard);
    }

    /**
     * @param key argument name
     * @return value or {@code null}
     */
    public String argument(String key) {
        return arguments.get(key);
    }
}
