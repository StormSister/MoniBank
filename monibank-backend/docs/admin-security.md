# MoniBank administrator security

Technical and maintenance endpoints live below `/api/admin` and require a
short-lived bearer token. Public educational workflows remain available under
their existing `/api` paths and continue to use the public rate limiter.

## Required secrets

The application intentionally has no committed administrator credentials or
JWT signing key. Set these values before startup:

```powershell
$env:MONIBANK_ADMIN_USERNAME = "monika"
$env:MONIBANK_ADMIN_PASSWORD = "replace-with-a-long-unique-password"

$jwtBytes = [byte[]]::new(32)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
$env:MONIBANK_JWT_SECRET = [Convert]::ToBase64String($jwtBytes)
```

The variables above live only in the current PowerShell process. When the
backend is started by IntelliJ, put the same three variables in the selected
Run Configuration. In production, provide them through the container's secret
or environment configuration. Never commit their values.

`MONIBANK_ADMIN_TOKEN_TTL` defaults to `PT1H`. The JWT secret must be a Base64
encoding of at least 32 random bytes.

## Get a token

```powershell
$login = @{
    username = $env:MONIBANK_ADMIN_USERNAME
    password = $env:MONIBANK_ADMIN_PASSWORD
} | ConvertTo-Json

$auth = Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/admin/auth/token" `
    -ContentType "application/json" `
    -Body $login

$adminHeaders = @{
    Authorization = "Bearer $($auth.accessToken)"
}

$auth | Format-List tokenType, expiresIn, expiresAt
```

The token response contains `tokenType: Bearer` and `expiresIn: 3600` with the
default configuration.

## Verify access rules

Without a token, a technical endpoint returns `401`:

```powershell
curl.exe -i "http://localhost:8080/api/admin/mainframe/jobs/NOTFOUND"
```

With the token, Spring Security admits the request. `NOTFOUND` itself returns
`404`, which confirms that authentication succeeded:

```powershell
curl.exe -i `
    -H "Authorization: Bearer $($auth.accessToken)" `
    "http://localhost:8080/api/admin/mainframe/jobs/NOTFOUND"
```

Public endpoints remain accessible without a token:

```powershell
curl.exe -i "http://localhost:8080/api/mainframe/status"
```

The daily close is now called with the administrator header:

```powershell
Invoke-RestMethod `
    -Method Post `
    -Uri "http://localhost:8080/api/admin/daily-close?date=2026-09-13&currency=EUR" `
    -Headers $adminHeaders
```

## Rate limiting

- Public reads and writes retain their separate limits.
- `/api/admin/auth/token` allows a small, independent burst of sign-in attempts.
- Requests carrying a valid administrator JWT bypass the public rate limiter.
- Invalid, missing and expired tokens never receive administrator access.

The authentication limit can be adjusted with
`MONIBANK_AUTH_LIMIT_CAPACITY`, `MONIBANK_AUTH_LIMIT_REFILL_TOKENS` and
`MONIBANK_AUTH_LIMIT_REFILL_PERIOD`.
