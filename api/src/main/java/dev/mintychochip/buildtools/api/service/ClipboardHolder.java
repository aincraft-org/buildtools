package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import java.util.Optional;

/**
 * Per-actor unnamed clipboard (copy / load destination).
 */
public interface ClipboardHolder {
    /**
     * @param actor player
     * @param clipboard new clipboard
     */
    void setClipboard(ActorId actor, Clipboard clipboard);

    /**
     * @param actor player
     * @return current clipboard if any
     */
    Optional<Clipboard> clipboard(ActorId actor);
}
