package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.blueprint.BlueprintMeta;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import java.util.List;
import java.util.Optional;

public interface BlueprintStore {
    void save(ActorId owner, String name, Clipboard clipboard);

    Optional<Clipboard> load(ActorId owner, String name);

    List<BlueprintMeta> list(ActorId owner);

    boolean delete(ActorId owner, String name);
}
