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

    private static String requirePrintableAsciiInput(
            String input
    ) {

        if (input == null) {
            throw new IllegalArgumentException(
                    "input cannot be null."
            );
        }

        if (input.length() > 512) {
            throw new IllegalArgumentException(
                    "input cannot exceed 512 characters."
            );
        }

        for (int position = 0;
             position < input.length();
             position++) {

            char character =
                    input.charAt(position);

            if (character < 32
                    || character > 126) {

                throw new IllegalArgumentException(
                        "input must contain printable ASCII "
                                + "characters only; invalid character "
                                + "at position "
                                + position
                                + "."
                );
            }
        }

        return input;
    }
}