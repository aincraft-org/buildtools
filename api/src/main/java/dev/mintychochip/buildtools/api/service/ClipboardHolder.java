package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.clipboard.Clipboard;
import java.util.Optional;

public interface ClipboardHolder {
    void setClipboard(ActorId actor, Clipboard clipboard);

    Optional<Clipboard> clipboard(ActorId actor);
}
