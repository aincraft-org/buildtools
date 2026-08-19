package dev.mintychochip.buildtools.api.tool;

import java.util.List;
import java.util.Objects;

/**
 * Valid/invalid outcome with an immutable error list.
 *
 * <p>The factory is {@link #passed()} because a record cannot also expose a static
 * {@code valid()} method.
 *
 * @param success whether the check passed
 * @param errors empty iff {@code success} is {@code true}
 */
public record ValidationResult(boolean success, List<String> errors) {
    /**
     * @throws NullPointerException if {@code errors} is {@code null}
     * @throws IllegalArgumentException if success and errors disagree
     */
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

    /** @return {@code true} if the check passed */
    public boolean valid() {
        return success;
    }

    /** @return a valid result with no errors */
    public static ValidationResult passed() {
        return new ValidationResult(true, List.of());
    }

    /**
     * @param error single error
     * @return invalid result
     */
    public static ValidationResult invalid(String error) {
        return new ValidationResult(false, List.of(error));
    }

    /**
     * @param errors at least one error
     * @return invalid result
     */
    public static ValidationResult invalid(List<String> errors) {
        return new ValidationResult(false, errors);
    }

    /** @return first error, or empty string if none */
    public String firstError() {
        return errors.isEmpty() ? "" : errors.getFirst();
    }
}
