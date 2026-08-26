package dev.mintychochip.masonry.paper.adapter;

import dev.mintychochip.masonry.api.world.BlockState;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.bukkit.block.data.BlockData;

/**
 * Translates Bukkit {@link BlockData} strings into API {@link BlockState} values and back.
 * Property keys are serialized in sorted order for stable equality.
 */
public final class PaperBlockStates {
    private PaperBlockStates() {}

    /**
     * @param data Bukkit block data
     * @return API state
     */
    public static BlockState fromBukkit(BlockData data) {
        return parse(data.getAsString());
    }

    /**
     * @param raw {@code namespaced:key} or {@code namespaced:key[k=v,...]}
     * @return API state
     */
    public static BlockState parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("namespacedKey must be present");
        }
        int bracket = raw.indexOf('[');
        if (bracket < 0) {
            return BlockState.of(raw);
        }
        if (!raw.endsWith("]")) {
            throw new IllegalArgumentException("Malformed block state: " + raw);
        }
        String key = raw.substring(0, bracket);
        String body = raw.substring(bracket + 1, raw.length() - 1);
        Map<String, String> properties = new LinkedHashMap<>();
        if (!body.isBlank()) {
            for (String part : body.split(",")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) {
                    throw new IllegalArgumentException("Malformed block state property: " + part);
                }
                properties.put(kv[0], kv[1]);
            }
        }
        return new BlockState(key, properties);
    }

    /**
     * @param state API state
     * @return Bukkit {@link BlockData} string
     */
    public static String toBukkitString(BlockState state) {
        if (state.properties().isEmpty()) {
            return state.namespacedKey();
        }
        String props = state.properties().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
        return state.namespacedKey() + "[" + props + "]";
    }
}
