package com.monibank.mainframe.security;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/admin/auth")
public final class AdminAuthenticationController {

    private final AuthenticationManager authenticationManager;
    private final AdminTokenService tokenService;

    public AdminAuthenticationController(
            AuthenticationManager authenticationManager,
            AdminTokenService tokenService
    ) {
        this.authenticationManager = authenticationManager;
        this.tokenService = tokenService;
    }

    @PostMapping("/token")
    public ResponseEntity<?> token(
            @Valid @RequestBody AdminLoginRequest request
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.username(),
                            request.password()
                    )
            );
            AdminTokenService.IssuedAdminToken token =
                    tokenService.issue(authentication.getName());

            return ResponseEntity.ok(new AdminTokenResponse(
                    token.value(),
                    "Bearer",
                    token.expiresInSeconds(),
                    token.expiresAt()
            ));
        } catch (AuthenticationException exception) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new SecurityErrorResponse(
                            Instant.now(),
                            HttpStatus.UNAUTHORIZED.value(),
                            "INVALID_CREDENTIALS",
                            "Invalid administrator credentials.",
                            false
                    )
            );
        }
    }

    public record AdminLoginRequest(
            @NotBlank String username,
            @NotBlank String password
    ) {
    }

    public record AdminTokenResponse(
            String accessToken,
            String tokenType,
            long expiresIn,
            Instant expiresAt
    ) {
    }
}
