package dev.mintychochip.buildtools.common.blueprint;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.blueprint.BlueprintMeta;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.service.BlueprintStore;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class MemoryBlueprintStore implements BlueprintStore {
    private final Map<ActorId, Map<String, Stored>> byOwner = new LinkedHashMap<>();

    @Override
    public void save(ActorId owner, String name, Clipboard clipboard) {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(clipboard, "clipboard");
        if (name.isBlank()) {
            throw new IllegalArgumentException("name must be present");
        }
        SchematicCodec.Dimensions dimensions = SchematicCodec.dimensionsOf(clipboard);
        Stored stored = new Stored(
                new BlueprintMeta(name, owner, Instant.now(), dimensions.width(), dimensions.height(), dimensions.length()),
                clipboard);
        byOwner.computeIfAbsent(owner, ignored -> new LinkedHashMap<>()).put(name, stored);
    }

    @Override
    public Optional<Clipboard> load(ActorId owner, String name) {
        return Optional.ofNullable(byOwner.get(owner)).map(map -> map.get(name)).map(Stored::clipboard);
    }

    @Override
    public List<BlueprintMeta> list(ActorId owner) {
        Map<String, Stored> owned = byOwner.get(owner);
        if (owned == null) {
            return List.of();
        }
        List<BlueprintMeta> metas = new ArrayList<>();
        owned.values().forEach(stored -> metas.add(stored.meta));
        return List.copyOf(metas);
    }

    @Override
    public boolean delete(ActorId owner, String name) {
        Map<String, Stored> owned = byOwner.get(owner);
        if (owned == null) {
            return false;
        }
        return owned.remove(name) != null;
    }

    private record Stored(BlueprintMeta meta, Clipboard clipboard) {}
}
