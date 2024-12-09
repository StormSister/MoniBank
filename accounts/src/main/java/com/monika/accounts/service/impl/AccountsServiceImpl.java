package com.monika.accounts.service.impl;

import com.monika.accounts.constans.AccountsConstants;
import com.monika.accounts.dto.CustomerDto;
import com.monika.accounts.entity.Accounts;
import com.monika.accounts.entity.Customer;
import com.monika.accounts.exception.CustomerAlreadyExistsException;
import com.monika.accounts.mapper.CustomerMapper;
import com.monika.accounts.repository.AccountsRepository;
import com.monika.accounts.repository.CustomerRepository;
import com.monika.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@AllArgsConstructor
@Service
public class AccountsServiceImpl implements IAccountsService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;


    @Override
    public void createAccount(CustomerDto customerDto) {
        Optional<Customer> optionalCustomer = customerRepository.findByMobileNumber(customerDto.getMobileNumber());
        if(optionalCustomer.isPresent()) {
            throw new CustomerAlreadyExistsException("Customer already registered with given mobileNumber "
                    +customerDto.getMobileNumber());
        }
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        customer.setCreatedBy("Anonymous");
        customer.setCreatedAt(LocalDateTime.now());
        Customer savedCustomer = customerRepository.save(customer);
        accountsRepository.save(createNewAccount(savedCustomer));
    }

    /**
     * @param customer - Customer Object
     * @return the new account details
     */
    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);
        newAccount.setCreatedBy("Anonymous");
        newAccount.setCreatedAt(LocalDateTime.now());
        return newAccount;
    }
}
