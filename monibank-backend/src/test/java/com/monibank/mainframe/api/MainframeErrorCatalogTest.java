package com.monibank.mainframe.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainframeErrorCatalogTest {

    @Test
    void mapsCobolValidationErrorToUnprocessableContent() {
        MainframeErrorCatalog.ErrorDescriptor descriptor =
                MainframeErrorCatalog.describe("STLIMITNOTZERO");

        assertEquals(HttpStatus.UNPROCESSABLE_CONTENT, descriptor.status());
        assertEquals(
                "A standard account must have a zero overdraft limit.",
                descriptor.message()
        );
        assertFalse(descriptor.retryable());
    }

    @Test
    void mapsDuplicateToConflict() {
        MainframeErrorCatalog.ErrorDescriptor descriptor =
                MainframeErrorCatalog.describe("DUPIBAN");

        assertEquals(HttpStatus.CONFLICT, descriptor.status());
        assertFalse(descriptor.retryable());
    }

    @Test
    void mapsTechnicalCobolFailureToRetryableUnavailable() {
        MainframeErrorCatalog.ErrorDescriptor descriptor =
                MainframeErrorCatalog.describe("SEQREADFAIL");

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, descriptor.status());
        assertTrue(descriptor.retryable());
    }
}
