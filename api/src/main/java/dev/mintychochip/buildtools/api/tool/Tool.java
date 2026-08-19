package dev.mintychochip.buildtools.api.tool;

import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.service.SurvivalTransaction;
import dev.mintychochip.buildtools.api.service.WorldAccess;

public interface Tool {
    String name();

    ToolPreview preview(ToolRequest request, WorldAccess world);

    ValidationResult validate(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    OperationRecord execute(ToolRequest request, WorldAccess world, SurvivalTransaction survival);

    void undo(OperationRecord record, WorldAccess world, SurvivalTransaction survival);
}
