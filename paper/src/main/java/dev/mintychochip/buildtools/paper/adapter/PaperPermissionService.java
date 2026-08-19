package dev.mintychochip.buildtools.paper.adapter;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.PermissionService;
import java.util.Objects;
import org.bukkit.Server;
import org.bukkit.entity.Player;

/**
 * Online-player permission lookup. Offline actors have no nodes.
 */
public final class PaperPermissionService implements PermissionService {
    private final Server server;

    /**
     * @param server running server
     */
    public PaperPermissionService(Server server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    @Override
    public boolean has(ActorId actor, String node) {
        Player player = server.getPlayer(actor.value());
        return player != null && player.hasPermission(node);
    }
}
