package online.topla.ingestion.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private ValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static ValidationResult ok() {
        return new ValidationResult(true, List.of());
    }

    public static ValidationResult failure(String message) {
        return new ValidationResult(false, List.of(message));
    }

    public static ValidationResult failures(List<String> errors) {
        return new ValidationResult(errors.isEmpty(), List.copyOf(errors));
    }

    public static Builder builder() {
        return new Builder();
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    public static final class Builder {
        private final List<String> errors = new ArrayList<>();

        public Builder add(String error) {
            if (error != null && !error.isBlank()) {
                errors.add(error);
            }
            return this;
        }

        public ValidationResult build() {
            return new ValidationResult(errors.isEmpty(), new ArrayList<>(errors));
        }
    }
}
