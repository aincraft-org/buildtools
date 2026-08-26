package dev.mintychochip.masonry.common.blueprint;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.blueprint.BlueprintMeta;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.service.BlueprintStore;
import dev.mintychochip.masonry.api.service.ClipboardHolder;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Save/load/list/delete facade that loads named blueprints onto the actor clipboard.
 */
public final class BlueprintManager {
    private final BlueprintStore store;
    private final ClipboardHolder clipboards;

    /**
     * @param store persistence
     * @param clipboards session clipboard
     */
    public BlueprintManager(BlueprintStore store, ClipboardHolder clipboards) {
        this.store = Objects.requireNonNull(store, "store");
        this.clipboards = Objects.requireNonNull(clipboards, "clipboards");
    }

    /**
     * @param owner owner
     * @param name blueprint name
     * @param clipboard body
     */
    public void save(ActorId owner, String name, Clipboard clipboard) {
        store.save(owner, name, clipboard);
    }

    /**
     * Loads {@code name} into the owner's clipboard if present.
     *
     * @param owner owner
     * @param name blueprint name
     * @return loaded clipboard
     */
    public Optional<Clipboard> loadToClipboard(ActorId owner, String name) {
        Optional<Clipboard> loaded = store.load(owner, name);
        loaded.ifPresent(clipboard -> clipboards.setClipboard(owner, clipboard));
        return loaded;
    }

    /** @param owner owner @return listing */
    public List<BlueprintMeta> list(ActorId owner) {
        return store.list(owner);
    }

    /**
     * @param owner owner
     * @param name blueprint name
     * @return {@code true} if deleted
     */
    public boolean delete(ActorId owner, String name) {
        return store.delete(owner, name);
    }
}
