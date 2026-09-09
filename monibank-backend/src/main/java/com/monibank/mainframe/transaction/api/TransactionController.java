package com.monibank.mainframe.transaction.api;

import com.monibank.mainframe.transaction.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
@Validated
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/deposits")
    public ResponseEntity<TransactionResponse> deposit(
            @Valid @RequestBody CashTransactionRequest request
    ) {

        return ResponseEntity.ok(
                transactionService.deposit(request)
        );
    }

    @PostMapping("/withdrawals")
    public ResponseEntity<TransactionResponse> withdraw(
            @Valid @RequestBody CashTransactionRequest request
    ) {

        return ResponseEntity.ok(
                transactionService.withdraw(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getTransactions() {

        return ResponseEntity.ok(
                transactionService.getTransactions()
        );
    }

    @GetMapping("/recent")
    public ResponseEntity<List<TransactionResponse>> getRecentTransactions(
            @RequestParam(defaultValue = "5")
            @Min(1)
            @Max(50)
            int limit
    ) {

        return ResponseEntity.ok(
                transactionService.getRecentTransactions(limit)
        );
    }

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<List<TransactionResponse>> getTransactions(
            @PathVariable
            @Pattern(regexp = "A\\d{12}")
            String accountId
    ) {

        return ResponseEntity.ok(
                transactionService.getTransactions(accountId)
        );
    }
}
