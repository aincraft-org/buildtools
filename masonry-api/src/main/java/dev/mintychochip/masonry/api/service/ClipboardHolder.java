package dev.mintychochip.masonry.api.service;

import dev.mintychochip.masonry.api.ActorId;
import dev.mintychochip.masonry.api.clipboard.Clipboard;
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
