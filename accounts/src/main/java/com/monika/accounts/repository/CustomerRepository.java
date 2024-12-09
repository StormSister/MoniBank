package com.monika.accounts.repository;

import com.monika.accounts.entity.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long>  {
    Optional<Customer> findByMobileNumber(String mobileNumber);
}
