package dev.mintychochip.buildtools.common.support;

import dev.mintychochip.buildtools.api.ActorId;
import dev.mintychochip.buildtools.api.service.PermissionService;
import java.util.HashSet;
import java.util.Set;

public final class MapPermissions implements PermissionService {
    private final Set<String> allowed = new HashSet<>();
    private boolean allowAll = true;

    public MapPermissions allowAll() {
        allowAll = true;
        return this;
    }

    public MapPermissions denyAll() {
        allowAll = false;
        allowed.clear();
        return this;
    }

    public MapPermissions grant(String node) {
        allowAll = false;
        allowed.add(node);
        return this;
    }

    @Override
    public boolean has(ActorId actor, String node) {
        return allowAll || allowed.contains(node);
    }
}
