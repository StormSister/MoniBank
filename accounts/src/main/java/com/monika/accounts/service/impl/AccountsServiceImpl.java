package com.monika.accounts.service.impl;

import com.monika.accounts.dto.CustomerDto;
import com.monika.accounts.repository.AccountRepository;
import com.monika.accounts.repository.CustomerRepository;
import com.monika.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private AccountRepository accountRepository;
    private CustomerRepository customerRepository;


    @Override
    public void createAccount(CustomerDto customerDto) {
        // Implement logic to create an account and return the status and message

    }
}