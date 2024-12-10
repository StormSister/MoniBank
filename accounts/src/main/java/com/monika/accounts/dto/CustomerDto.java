package com.monika.accounts.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CustomerDto {

    @NotEmpty(message = "Name cannot be null or empty")
    @Size(min =3, max =30, message = "The length of the customer name must be between 3 and 30 characters")
    private String name;

    @Email(message = "Email address should be valid value")
    @NotEmpty(message = "Email cannot be null or empty")
    private String email;

    @Pattern(regexp= ("^[0-9]{10}$"), message = "Mobile number should be 10 digits and should not contain any special characters such as ^$|]")
    private String mobileNumber;

    private AccountsDto accountsDto;
}
