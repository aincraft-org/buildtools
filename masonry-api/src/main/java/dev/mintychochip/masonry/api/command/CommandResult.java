package dev.mintychochip.masonry.api.command;

import dev.mintychochip.masonry.api.operation.OperationRecord;
import dev.mintychochip.masonry.api.tool.ToolPreview;
import dev.mintychochip.masonry.api.tool.ValidationResult;
import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of {@code /masonry}, including validation text and optional preview/record.
 *
 * @param success whether the command completed its intended action
 * @param message player-facing summary
 * @param validation structured validation (always present)
 * @param preview planned mutation, or {@code null}
 * @param record executed operation, or {@code null}
 */
public record CommandResult(
        boolean success,
        String message,
        ValidationResult validation,
        ToolPreview preview,
        OperationRecord record) {
    /**
     * @throws NullPointerException if {@code message} or {@code validation} is {@code null}
     */
    public CommandResult {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(validation, "validation");
    }

    /**
     * Successful non-mutating result (for example setting pos1).
     *
     * @param message player text
     * @return success result
     */
    public static CommandResult ok(String message) {
        return new CommandResult(true, message, ValidationResult.passed(), null, null);
    }

    /**
     * Failed command with a single error message.
     *
     * @param message player text
     * @return failure result
     */
    public static CommandResult fail(String message) {
        return new CommandResult(false, message, ValidationResult.invalid(message), null, null);
    }

    /**
     * Failed command from an already-invalid validation.
     *
     * @param validation invalid result
     * @return failure result
     * @throws IllegalArgumentException if {@code validation} is valid
     */
    public static CommandResult invalid(ValidationResult validation) {
        Objects.requireNonNull(validation, "validation");
        if (validation.valid()) {
            throw new IllegalArgumentException("validation must be invalid");
        }
        return new CommandResult(false, validation.firstError(), validation, null, null);
    }

    /**
     * Successful mutating command.
     *
     * @param message player text
     * @param preview planned set
     * @param record applied record
     * @return success with preview and record
     */
    public static CommandResult executed(String message, ToolPreview preview, OperationRecord record) {
        return new CommandResult(true, message, ValidationResult.passed(), preview, record);
    }

    /** @return optional preview */
    public Optional<ToolPreview> previewOptional() {
        return Optional.ofNullable(preview);
    }

    /** @return optional operation record */
    public Optional<OperationRecord> recordOptional() {
        return Optional.ofNullable(record);
    }
}
