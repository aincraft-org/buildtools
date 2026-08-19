package dev.mintychochip.buildtools.api.tool;

import java.util.List;
import java.util.Objects;

public record ValidationResult(boolean success, List<String> errors) {
    public ValidationResult {
        Objects.requireNonNull(errors, "errors");
        errors = List.copyOf(errors);
        if (success && !errors.isEmpty()) {
            throw new IllegalArgumentException("Valid results cannot include errors");
        }
        if (!success && errors.isEmpty()) {
            throw new IllegalArgumentException("Invalid results require at least one error");
        }
    }

    public boolean valid() {
        return success;
    }

    public static ValidationResult passed() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error));
    }

    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    public String firstError() {
        return errors.isEmpty() ? "" : errors.getFirst();
    }
}
