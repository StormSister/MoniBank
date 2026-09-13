package com.monibank.mainframe;

import com.monibank.mainframe.account.AccountService;
import com.monibank.mainframe.account.api.AccountResponse;
import com.monibank.mainframe.statement.AccountStatementService;

import com.monibank.mainframe.statement.api.AccountStatementResponse;
import com.monibank.mainframe.transaction.TransactionService;
import com.monibank.mainframe.transaction.api.TransactionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AccountStatementServiceTest {

    private static final String ACCOUNT_ID = "A000000000002";

    private AccountService accountService;
    private TransactionService transactionService;
    private AccountStatementService service;

    @BeforeEach
    void setUp() {
        accountService = mock(AccountService.class);
        transactionService = mock(TransactionService.class);
        service = new AccountStatementService(
                accountService,
                transactionService
        );

        when(accountService.getAccount(ACCOUNT_ID)).thenReturn(
                new AccountResponse(
                        ACCOUNT_ID,
                        "C000000000006",
                        "PL84999999990000000000000002",
                        "OD",
                        "EUR",
                        new BigDecimal("135.00"),
                        new BigDecimal("1000.00"),
                        BigDecimal.ZERO,
                        "A"
                )
        );
    }

    @Test
    void buildsStatementAndCalculatesBalances() {
        when(transactionService.getTransactions(ACCOUNT_ID)).thenReturn(
                List.of(
                        transaction(
                                "T000000000003",
                                "D",
                                "15.00",
                                "135.00",
                                "20260912120000"
                        ),
                        transaction(
                                "T000000000001",
                                "C",
                                "100.00",
                                "100.00",
                                "20260910120000"
                        ),
                        transaction(
                                "T000000000002",
                                "C",
                                "50.00",
                                "150.00",
                                "20260911120000"
                        )
                )
        );

        AccountStatementResponse statement = service.generate(
                ACCOUNT_ID,
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 12)
        );

        assertThat(statement.summary().openingBalance())
                .isEqualByComparingTo("100.00");
        assertThat(statement.summary().totalCredits())
                .isEqualByComparingTo("50.00");
        assertThat(statement.summary().totalDebits())
                .isEqualByComparingTo("15.00");
        assertThat(statement.summary().closingBalance())
                .isEqualByComparingTo("135.00");
        assertThat(statement.summary().transactionCount()).isEqualTo(2);
        assertThat(statement.transactions())
                .extracting(TransactionResponse::transactionId)
                .containsExactly(
                        "T000000000002",
                        "T000000000003"
                );
    }

    @Test
    void returnsLastKnownBalanceForEmptyPeriod() {
        when(transactionService.getTransactions(ACCOUNT_ID)).thenReturn(
                List.of(transaction(
                        "T000000000001",
                        "C",
                        "100.00",
                        "100.00",
                        "20260910120000"
                ))
        );

        AccountStatementResponse statement = service.generate(
                ACCOUNT_ID,
                LocalDate.of(2026, 9, 11),
                LocalDate.of(2026, 9, 12)
        );

        assertThat(statement.summary().openingBalance())
                .isEqualByComparingTo("100.00");
        assertThat(statement.summary().closingBalance())
                .isEqualByComparingTo("100.00");
        assertThat(statement.transactions()).isEmpty();
    }

    @Test
    void rejectsInvalidPeriod() {
        assertThatThrownBy(() -> service.generate(
                ACCOUNT_ID,
                LocalDate.of(2026, 9, 12),
                LocalDate.of(2026, 9, 11)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be after");
    }

    private TransactionResponse transaction(
            String transactionId,
            String direction,
            String amount,
            String balanceAfter,
            String createdAt
    ) {
        return new TransactionResponse(
                transactionId,
                ACCOUNT_ID,
                direction,
                "DP",
                "EUR",
                new BigDecimal(amount),
                new BigDecimal(balanceAfter),
                "TEST",
                "CASHDESK00001",
                createdAt,
                "C"
        );
    }
}
