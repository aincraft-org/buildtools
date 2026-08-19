package dev.mintychochip.buildtools.common.session;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import dev.mintychochip.buildtools.api.service.ClipboardHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class PlayerSessionStore implements ClipboardHolder {
    private final Map<ActorId, PlayerSession> sessions = new HashMap<>();

    public PlayerSession session(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        return sessions.computeIfAbsent(actor, ignored -> new PlayerSession());
    }

    public void remove(ActorId actor) {
        sessions.remove(actor);
    }

    @Override
    public void setClipboard(ActorId actor, Clipboard clipboard) {
        session(actor).setClipboard(clipboard);
    }

    @Override
    public Optional<Clipboard> clipboard(ActorId actor) {
        return Optional.ofNullable(sessions.get(actor)).flatMap(PlayerSession::clipboard);
    }
}
