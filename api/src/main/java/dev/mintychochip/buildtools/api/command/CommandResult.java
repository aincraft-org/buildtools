package dev.mintychochip.buildtools.api.command;

import dev.mintychochip.buildtools.api.operation.OperationRecord;
import dev.mintychochip.buildtools.api.tool.ToolPreview;
import dev.mintychochip.buildtools.api.tool.ValidationResult;
import java.util.Objects;
import java.util.Optional;

public record CommandResult(
        boolean success,
        String message,
        ValidationResult validation,
        ToolPreview preview,
        OperationRecord record) {
    public CommandResult {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(validation, "validation");
    }

    public static CommandResult ok(String message) {
        return new CommandResult(true, message, ValidationResult.passed(), null, null);
    }

    public static CommandResult fail(String message) {
        return new CommandResult(false, message, ValidationResult.invalid(message), null, null);
    }

    public static CommandResult invalid(ValidationResult validation) {
        Objects.requireNonNull(validation, "validation");
        if (validation.valid()) {
            throw new IllegalArgumentException("validation must be invalid");
        }
        return new CommandResult(false, validation.firstError(), validation, null, null);
    }

    public static CommandResult executed(String message, ToolPreview preview, OperationRecord record) {
        return new CommandResult(true, message, ValidationResult.passed(), preview, record);
    }

    public Optional<ToolPreview> previewOptional() {
        return Optional.ofNullable(preview);
    }

    public Optional<OperationRecord> recordOptional() {
        return Optional.ofNullable(record);
    }
}
