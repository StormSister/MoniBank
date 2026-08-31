package com.monibank.mainframe.hercules.terminal;

public record MbgwRequest(
        String operation,
        String requestId,
        String input
) {

    private static final int MAX_OPERATION_LENGTH = 8;
    private static final int REQUEST_ID_LENGTH = 8;
    private static final int MAX_INPUT_LENGTH = 512;

    public MbgwRequest {
        requireAlphanumeric(
                "operation",
                operation,
                1,
                MAX_OPERATION_LENGTH
        );

        requireAlphanumeric(
                "requestId",
                requestId,
                REQUEST_ID_LENGTH,
                REQUEST_ID_LENGTH
        );

        requirePrintableAsciiInput(input);
    }

    public String formattedInputLength() {
        return String.format("%04d", input.length());
    }

    private static void requireAlphanumeric(
            String name,
            String value,
            int minimumLength,
            int maximumLength
    ) {
        if (value == null
                || value.length() < minimumLength
                || value.length() > maximumLength
                || !value.chars().allMatch(Character::isLetterOrDigit)) {
            throw new IllegalArgumentException(
                    name + " must contain "
                            + minimumLength + "-"
                            + maximumLength
                            + " alphanumeric characters."
            );
        }
    }

    private static void requirePrintableAsciiInput(String input) {
        if (input == null
                || input.isEmpty()
                || input.length() > MAX_INPUT_LENGTH) {
            throw new IllegalArgumentException(
                    "input must contain 1-"
                            + MAX_INPUT_LENGTH
                            + " characters."
            );
        }

        boolean printableAscii = input.chars()
                .allMatch(character ->
                        character >= 32 && character <= 126
                );

        if (!printableAscii) {
            throw new IllegalArgumentException(
                    "input must contain printable ASCII characters only."
            );
        }
    }
}