package com.monibank.mainframe.customer.api;

public record CustomerResponse(
        String customerId,
        String countryCode,
        String nationalId,
        String firstName,
        String lastName,
        String dateOfBirth,
        String status,
        String createdAt
) {
}