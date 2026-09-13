package com.monibank.mainframe.statement.api;

import com.monibank.mainframe.statement.AccountStatementService;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/statements")
@RequiredArgsConstructor
@Validated
public class AccountStatementController {

    private final AccountStatementService accountStatementService;

    @GetMapping("/accounts/{accountId}")
    public ResponseEntity<AccountStatementResponse> getStatement(
            @PathVariable
            @Pattern(regexp = "A\\d{12}")
            String accountId,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to
    ) {

        return ResponseEntity.ok(
                accountStatementService.generate(
                        accountId,
                        from,
                        to
                )
        );
    }
}
