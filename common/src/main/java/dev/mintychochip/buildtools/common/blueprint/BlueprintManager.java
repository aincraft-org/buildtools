package dev.mintychochip.buildtools.common.blueprint;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.blueprint.BlueprintMeta;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.service.BlueprintStore;
import dev.mintychochip.buildtools.api.service.ClipboardHolder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BlueprintManager {
    private final BlueprintStore store;
    private final ClipboardHolder clipboards;

    public BlueprintManager(BlueprintStore store, ClipboardHolder clipboards) {
        this.store = Objects.requireNonNull(store, "store");
        this.clipboards = Objects.requireNonNull(clipboards, "clipboards");
    }

    public void save(ActorId owner, String name, Clipboard clipboard) {
        store.save(owner, name, clipboard);
    }

    public Optional<Clipboard> loadToClipboard(ActorId owner, String name) {
        Optional<Clipboard> loaded = store.load(owner, name);
        loaded.ifPresent(clipboard -> clipboards.setClipboard(owner, clipboard));
        return loaded;
    }

    public List<BlueprintMeta> list(ActorId owner) {
        return store.list(owner);
    }

    public boolean delete(ActorId owner, String name) {
        return store.delete(owner, name);
    }
}
