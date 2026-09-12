package com.monibank.mainframe.api;

import org.springframework.http.HttpStatus;

import java.util.Locale;

final class MainframeErrorCatalog {

    private MainframeErrorCatalog() {
    }

    static ErrorDescriptor describe(String rawCode) {
        String code = normalize(rawCode);

        return switch (code) {
            case "NOTFOUND" -> notFound("The requested record was not found.");
            case "CUSTOMERNOTFOUND" -> notFound("The customer was not found.");
            case "ACCOUNTNOTFOUND" -> notFound("The account was not found.");
            case "RPTNOTF" -> notFound("The requested report was not found.");

            case "DUPCUSTOMERID", "DUPKEY" ->
                    conflict("A customer with these details already exists.");
            case "DUPACCOUNTID" ->
                    conflict("The generated account ID already exists.");
            case "DUPIBAN" ->
                    conflict("The generated IBAN already exists.");
            case "DUPCARDID" ->
                    conflict("The generated card ID already exists.");
            case "DUPCARDNUMBER" ->
                    conflict("The generated card number already exists.");
            case "DUPTXNID" ->
                    conflict("The generated transaction ID already exists.");

            case "CUSTOMERINACTIVE" ->
                    conflict("The customer is inactive.");
            case "ACCOUNTINACTIVE" ->
                    conflict("The account is inactive.");
            case "INSUFFICIENTFUNDS" ->
                    conflict("The account has insufficient available funds.");
            case "TOOMANYACCOUNTS" ->
                    conflict("The customer has reached the account limit.");

            case "STLIMITNOTZERO" ->
                    unprocessable("A standard account must have a zero overdraft limit.");
            case "SEQEXHAUSTED" ->
                    unavailable("The mainframe ID range is exhausted.", false);

            case "INTERNAL", "READERR", "ACCOUNTREADFAIL",
                    "CUSTOMERREADFAIL", "SEQREADFAIL", "SEQREWRITEFAIL",
                    "SEQUNLOCKFAIL" ->
                    unavailable("The mainframe could not complete the operation.", true);

            default -> defaultDescriptor(code);
        };
    }

    private static ErrorDescriptor defaultDescriptor(String code) {
        if (code.startsWith("BAD")
                || code.endsWith("BADLENGTH")
                || code.endsWith("KEYERROR")
                || "KEYMISMATCH".equals(code)) {
            return unprocessable("The mainframe rejected the supplied data.");
        }

        if (code.startsWith("DUP")) {
            return conflict("The operation would create a duplicate record.");
        }

        if (code.endsWith("NOTFOUND")) {
            return notFound("The requested record was not found.");
        }

        return new ErrorDescriptor(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "The mainframe operation failed.",
                false
        );
    }

    private static String normalize(String code) {
        if (code == null || code.isBlank()) {
            return "MAINFRAME_ERROR";
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static ErrorDescriptor notFound(String message) {
        return new ErrorDescriptor(HttpStatus.NOT_FOUND, message, false);
    }

    private static ErrorDescriptor conflict(String message) {
        return new ErrorDescriptor(HttpStatus.CONFLICT, message, false);
    }

    private static ErrorDescriptor unprocessable(String message) {
        return new ErrorDescriptor(
                HttpStatus.UNPROCESSABLE_CONTENT,
                message,
                false
        );
    }

    private static ErrorDescriptor unavailable(
            String message,
            boolean retryable
    ) {
        return new ErrorDescriptor(
                HttpStatus.SERVICE_UNAVAILABLE,
                message,
                retryable
        );
    }

    record ErrorDescriptor(
            HttpStatus status,
            String message,
            boolean retryable
    ) {
    }
}
