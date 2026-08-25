package dev.mintychochip.masonry.api.tool;

import dev.mintychochip.masonry.api.operation.OperationRecord;
import dev.mintychochip.masonry.api.service.SurvivalTransaction;
import dev.mintychochip.masonry.api.service.WorldAccess;

/**
 * A registered building operation. Tools are stateless; per-player data lives on the request
 * and in session/clipboard ports.
 */
public interface Tool {
    /**
     * Registry key and permission suffix ({@code masonry.tool.<name>}).
     *
     * @return non-blank name
     */
    String name();

    /**
     * Plans the exact positions that {@link #execute} will change. The affected set must match
     * execute for mutating tools.
     *
     * @param request actor, selection, and arguments
     * @param world current world
     * @return preview region, affected positions, and estimated placement cost
     */
    ToolPreview preview(ToolRequest request, WorldAccess world);

    /**
     * Tool-specific argument and selection checks. Limit, permission, and affordability checks
     * live on the executor.
     *
     * @param request request
     * @param world world
     * @param survival inventory port
     * @return valid or a concrete error
     */
    ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    /**
     * Applies the planned mutation and returns the record of diffs actually written.
     *
     * <p>The executor charges before calling this method. If every {@code setBlock} is cancelled,
     * implementations must restore the world and return an empty change list so the executor can
     * refund and refuse.
     *
     * @param request request
     * @param world world
     * @param survival inventory port (tools do not charge here)
     * @return operation record
     */
    OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    /**
     * Restores {@code before} states from {@code record}. Charge/refund is handled by the executor.
     *
     * @param record previously executed record
     * @param world world
     * @param survival inventory port
     */
    void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival);
}
