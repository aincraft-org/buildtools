package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;

/**
 * Permission lookup. Tool use requires {@code buildtools.tool.<name>}.
 */
public interface PermissionService {
    /**
     * @param actor player
     * @param node permission node
     * @return {@code true} if the actor currently has {@code node}
     */
    boolean has(ActorId actor, String node);
}
