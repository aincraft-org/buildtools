package dev.mintychochip.masonry.common.session;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
import dev.mintychochip.masonry.api.service.ClipboardHolder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Lazy per-actor {@link PlayerSession} map that also implements {@link ClipboardHolder}.
 */
public final class PlayerSessionStore implements ClipboardHolder {
    private final Map<ActorId, PlayerSession> sessions = new HashMap<>();

    /**
     * @param actor player
     * @return existing or new session
     */
    public PlayerSession session(ActorId actor) {
        Objects.requireNonNull(actor, "actor");
        return sessions.computeIfAbsent(actor, ignored -> new PlayerSession());
    }

    /**
     * Drops the session (logout).
     *
     * @param actor player
     */
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
