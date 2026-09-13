package com.monibank.mainframe.statement;

import com.monibank.mainframe.account.AccountService;
import com.monibank.mainframe.account.api.AccountResponse;
import com.monibank.mainframe.statement.api.AccountStatementResponse;
import com.monibank.mainframe.transaction.TransactionService;
import com.monibank.mainframe.transaction.api.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AccountStatementService {

    private static final int MAX_PERIOD_DAYS = 366;
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AccountService accountService;
    private final TransactionService transactionService;

    public AccountStatementResponse generate(
            String accountId,
            LocalDate from,
            LocalDate to
    ) {

        validatePeriod(from, to);

        AccountResponse account =
                accountService.getAccount(accountId);

        List<TransactionResponse> allTransactions =
                transactionService.getTransactions(accountId)
                        .stream()
                        .sorted(statementOrder())
                        .toList();

        List<TransactionResponse> statementTransactions =
                allTransactions.stream()
                        .filter(transaction -> {
                            LocalDate transactionDate =
                                    transactionDate(transaction);
                            return !transactionDate.isBefore(from)
                                    && !transactionDate.isAfter(to);
                        })
                        .toList();

        BigDecimal openingBalance = openingBalance(
                allTransactions,
                statementTransactions,
                from
        );

        BigDecimal closingBalance = statementTransactions.isEmpty()
                ? openingBalance
                : statementTransactions.getLast().balanceAfter();

        BigDecimal totalCredits = total(
                statementTransactions,
                "C"
        );
        BigDecimal totalDebits = total(
                statementTransactions,
                "D"
        );

        return new AccountStatementResponse(
                new AccountStatementResponse.Account(
                        account.accountId(),
                        account.customerId(),
                        account.iban(),
                        account.type(),
                        account.currency(),
                        account.status()
                ),
                new AccountStatementResponse.Period(from, to),
                new AccountStatementResponse.Summary(
                        openingBalance,
                        totalCredits,
                        count(statementTransactions, "C"),
                        totalDebits,
                        count(statementTransactions, "D"),
                        closingBalance,
                        statementTransactions.size()
                ),
                statementTransactions,
                Instant.now()
        );
    }

    private void validatePeriod(
            LocalDate from,
            LocalDate to
    ) {

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Statement start and end dates are required."
            );
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Statement start date cannot be after end date."
            );
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_PERIOD_DAYS) {
            throw new IllegalArgumentException(
                    "Statement period cannot exceed 366 days."
            );
        }
    }

    private BigDecimal openingBalance(
            List<TransactionResponse> allTransactions,
            List<TransactionResponse> statementTransactions,
            LocalDate from
    ) {

        return allTransactions.stream()
                .filter(transaction ->
                        transactionDate(transaction).isBefore(from))
                .reduce((first, second) -> second)
                .map(TransactionResponse::balanceAfter)
                .orElseGet(() -> statementTransactions.isEmpty()
                        ? BigDecimal.ZERO
                        : balanceBefore(statementTransactions.getFirst()));
    }

    private BigDecimal balanceBefore(
            TransactionResponse transaction
    ) {

        BigDecimal signedAmount = "D".equals(transaction.direction())
                ? transaction.amount().negate()
                : transaction.amount();

        return transaction.balanceAfter().subtract(signedAmount);
    }

    private BigDecimal total(
            List<TransactionResponse> transactions,
            String direction
    ) {

        return transactions.stream()
                .filter(transaction ->
                        direction.equals(transaction.direction()))
                .map(TransactionResponse::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private long count(
            List<TransactionResponse> transactions,
            String direction
    ) {

        return transactions.stream()
                .filter(transaction ->
                        direction.equals(transaction.direction()))
                .count();
    }

    private Comparator<TransactionResponse> statementOrder() {

        return Comparator.comparing(TransactionResponse::createdAt)
                .thenComparing(TransactionResponse::transactionId);
    }

    private LocalDate transactionDate(
            TransactionResponse transaction
    ) {

        try {
            return LocalDate.parse(
                    transaction.createdAt(),
                    TIMESTAMP_FORMAT
            );
        } catch (DateTimeParseException exception) {
            throw new IllegalStateException(
                    "Transaction "
                            + transaction.transactionId()
                            + " contains an invalid timestamp.",
                    exception
            );
        }
    }
}
