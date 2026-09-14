package com.monibank.mainframe.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

@Component
public final class ApiRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(
            ApiRateLimitFilter.class
    );
    private static final String OVERFLOW_CLIENT = "__overflow__";
    private static final Pattern SAFE_CLIENT_ID = Pattern.compile(
            "[0-9A-Fa-f:.]{1,64}"
    );

    private final RateLimitProperties properties;
    private final ConcurrentMap<BucketKey, TokenBucket> buckets =
            new ConcurrentHashMap<>();

    public ApiRateLimitFilter(RateLimitProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled()
                || !request.getRequestURI().startsWith("/api/")
                || isAuthenticatedAdmin()
                || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        RequestScope scope = classify(request);
        RateLimitProperties.Policy policy = policy(scope);
        String clientId = clientId(request);
        BucketKey key = bucketKey(clientId, scope);
        long nowNanos = System.nanoTime();

        TokenBucket bucket = buckets.computeIfAbsent(
                key,
                ignored -> new TokenBucket(policy, nowNanos)
        );
        Decision decision = bucket.tryConsume(nowNanos);

        addHeaders(response, scope, policy, decision);
        if (decision.allowed()) {
            filterChain.doFilter(request, response);
            return;
        }

        log.info(
                "API rate limit exceeded: client={}, scope={}, method={}, path={}",
                clientId,
                scope,
                request.getMethod(),
                request.getRequestURI()
        );
        writeRejection(response, scope, decision.retryAfterSeconds());
    }

    @Scheduled(
            fixedDelayString =
                    "${monibank.rate-limit.cleanup-interval:PT5M}"
    )
    void removeIdleBuckets() {
        long cutoff = System.nanoTime() - properties.idleTtl().toNanos();
        buckets.entrySet().removeIf(
                entry -> entry.getValue().lastAccessNanos() < cutoff
        );
    }

    int trackedBucketCount() {
        return buckets.size();
    }

    private BucketKey bucketKey(String clientId, RequestScope scope) {
        BucketKey requested = new BucketKey(clientId, scope);
        if (buckets.containsKey(requested)
                || buckets.size() < properties.maxTrackedClients() * 3) {
            return requested;
        }
        return new BucketKey(OVERFLOW_CLIENT, scope);
    }

    private RequestScope classify(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod().toUpperCase(Locale.ROOT);
        if ("/api/admin/auth/token".equals(path)) {
            return RequestScope.AUTH;
        }
        if ("GET".equals(method)
                || "HEAD".equals(method)
                || ("POST".equals(method)
                && "/api/customers/get".equals(path))) {
            return RequestScope.READ;
        }
        return RequestScope.WRITE;
    }

    private RateLimitProperties.Policy policy(RequestScope scope) {
        return switch (scope) {
            case AUTH -> properties.auth();
            case READ -> properties.read();
            case WRITE -> properties.write();
        };
    }

    private static boolean isAuthenticatedAdmin() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "SCOPE_admin".equals(
                        authority.getAuthority()
                ));
    }

    private String clientId(HttpServletRequest request) {
        String headerName = properties.trustedClientIpHeader();
        if (headerName != null && !headerName.isBlank()) {
            String forwarded = request.getHeader(headerName);
            if (forwarded != null) {
                String candidate = forwarded.split(",", 2)[0].trim();
                if (SAFE_CLIENT_ID.matcher(candidate).matches()) {
                    return candidate;
                }
                log.warn("Ignored invalid trusted client IP header value");
            }
        }

        String remoteAddress = request.getRemoteAddr();
        return remoteAddress == null || remoteAddress.isBlank()
                ? "unknown"
                : remoteAddress;
    }

    private static void addHeaders(
            HttpServletResponse response,
            RequestScope scope,
            RateLimitProperties.Policy policy,
            Decision decision
    ) {
        response.setHeader("X-RateLimit-Scope", scope.name().toLowerCase());
        response.setHeader("X-RateLimit-Limit", String.valueOf(policy.capacity()));
        response.setHeader(
                "X-RateLimit-Remaining",
                String.valueOf(decision.remaining())
        );
        response.setHeader(
                "X-RateLimit-Reset",
                String.valueOf(Instant.now().getEpochSecond()
                        + decision.retryAfterSeconds())
        );
    }

    private static void writeRejection(
            HttpServletResponse response,
            RequestScope scope,
            long retryAfterSeconds
    ) throws IOException {
        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        String message = switch (scope) {
            case AUTH -> "Too many administrator sign-in attempts.";
            case READ -> "Too many read requests.";
            case WRITE -> "Too many operations that change demo data.";
        };

        response.getWriter().write(
                "{\"timestamp\":\"" + Instant.now()
                        + "\",\"status\":429"
                        + ",\"code\":\"RATE_LIMIT_EXCEEDED\""
                        + ",\"message\":\"" + message
                        + " Try again in " + retryAfterSeconds
                        + " seconds.\""
                        + ",\"retryable\":true}"
        );
    }

    private enum RequestScope {
        AUTH,
        READ,
        WRITE
    }

    private record BucketKey(String clientId, RequestScope scope) {
    }

    private record Decision(
            boolean allowed,
            int remaining,
            long retryAfterSeconds
    ) {
    }

    private static final class TokenBucket {

        private final int capacity;
        private final double tokensPerNano;
        private double tokens;
        private long lastRefillNanos;
        private volatile long lastAccessNanos;

        private TokenBucket(
                RateLimitProperties.Policy policy,
                long nowNanos
        ) {
            capacity = policy.capacity();
            tokensPerNano = (double) policy.refillTokens()
                    / policy.refillPeriod().toNanos();
            tokens = capacity;
            lastRefillNanos = nowNanos;
            lastAccessNanos = nowNanos;
        }

        private synchronized Decision tryConsume(long nowNanos) {
            refill(nowNanos);
            lastAccessNanos = nowNanos;

            if (tokens >= 1.0d) {
                tokens -= 1.0d;
                return new Decision(
                        true,
                        (int) Math.floor(tokens),
                        secondsUntilNextToken()
                );
            }

            return new Decision(false, 0, secondsUntilNextToken());
        }

        private void refill(long nowNanos) {
            long elapsed = Math.max(0L, nowNanos - lastRefillNanos);
            tokens = Math.min(capacity, tokens + elapsed * tokensPerNano);
            lastRefillNanos = nowNanos;
        }

        private long secondsUntilNextToken() {
            if (tokens >= 1.0d) {
                return 0L;
            }
            double missing = 1.0d - tokens;
            return Math.max(
                    1L,
                    (long) Math.ceil(
                            missing / tokensPerNano / 1_000_000_000.0d
                    )
            );
        }

        private long lastAccessNanos() {
            return lastAccessNanos;
        }
    }
}
