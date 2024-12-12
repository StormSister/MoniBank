package com.example.cards.repository;

import com.example.cards.entity.Cards;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CardsRepository extends JpaRepository<Cards, Long> {

    Optional<Cards> findByCardNumber(String cardNumber);

    Optional<Cards> findByMobileNumber(String mobileNumber);


}
