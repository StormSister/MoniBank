package com.monibank.mainframe.security;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public final class AdminTokenService {

    static final String ISSUER = "monibank";
    static final String ADMIN_SCOPE = "admin";

    private final JwtEncoder jwtEncoder;
    private final AdminSecurityProperties properties;

    public AdminTokenService(
            JwtEncoder jwtEncoder,
            AdminSecurityProperties properties
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public IssuedAdminToken issue(String username) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.tokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(ISSUER)
                .subject(username)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("scope", ADMIN_SCOPE)
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String accessToken = jwtEncoder.encode(
                JwtEncoderParameters.from(header, claims)
        ).getTokenValue();

        return new IssuedAdminToken(
                accessToken,
                properties.tokenTtl().toSeconds(),
                expiresAt
        );
    }

    public record IssuedAdminToken(
            String value,
            long expiresInSeconds,
            Instant expiresAt
    ) {
    }
}
