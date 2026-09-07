package com.monibank.mainframe.account.api;

import com.monibank.mainframe.account.AccountService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
@Validated
public class AccountController {

    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequest request
    ) {

        return ResponseEntity.ok(
                accountService.createAccount(request)
        );
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> getAccounts() {

        return ResponseEntity.ok(
                accountService.getAccounts()
        );
    }

    @PatchMapping("/{accountId}/status")
    public ResponseEntity<AccountResponse> changeStatus(
            @PathVariable
            @Pattern(regexp = "A\\d{12}")
            String accountId,
            @Valid @RequestBody ChangeAccountStatusRequest request
    ) {

        return ResponseEntity.ok(
                accountService.changeStatus(
                        accountId,
                        request.status()
                )
        );
    }
}
