final class ValidationResult {
    final boolean valid;
    final String errorMessage;

    private ValidationResult(boolean valid, String errorMessage) {
        this.valid = valid;
        this.errorMessage = errorMessage;
    }

    static ValidationResult ok() { return new ValidationResult(true, ""); }
    static ValidationResult fail(String msg) { return new ValidationResult(false, msg); }
}
