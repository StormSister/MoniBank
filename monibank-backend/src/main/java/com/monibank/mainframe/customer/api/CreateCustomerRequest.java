package com.monibank.mainframe.customer.api;



public record CreateCustomerRequest(
        String customerId,
        String countryCode,
        String nationalId,
        String firstName,
        String lastName,
        String dateOfBirth
) {
}