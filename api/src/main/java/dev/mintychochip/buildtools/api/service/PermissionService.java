package dev.mintychochip.buildtools.api.service;

import dev.mintychochip.buildtools.api.ActorId;

public interface PermissionService {
    boolean has(ActorId actor, String node);
}
