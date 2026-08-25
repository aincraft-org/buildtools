package dev.mintychochip.masonry.api.service;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.blueprint.BlueprintMeta;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import java.util.List;
import java.util.Optional;

/**
 * Player-scoped named blueprint persistence. Implementations must preserve block state, including air.
 */
public interface BlueprintStore {
    /**
     * Creates or overwrites {@code name} for {@code owner}.
     *
     * @param owner owner
     * @param name unique name for that owner
     * @param clipboard body
     */
    void save(ActorId owner, String name, Clipboard clipboard);

    /**
     * @param owner owner
     * @param name blueprint name
     * @return clipboard if it exists
     */
    Optional<Clipboard> load(ActorId owner, String name);

    /**
     * @param owner owner
     * @return this owner's blueprints
     */
    List<BlueprintMeta> list(ActorId owner);

    /**
     * @param owner owner
     * @param name blueprint name
     * @return {@code true} if something was removed
     */
    boolean delete(ActorId owner, String name);
}
