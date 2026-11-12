package com.example.unipathapi.auth.service;

import com.example.unipathapi.auth.exception.GoogleServiceUnavailableException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Provider responsible for fetching, caching, and rotating Google JWKS RSA public keys.
 *
 * Rules:
 * - Thread-safe in-memory cache using ConcurrentHashMap.
 * - Cache duration: 1 hour.
 * - Key rotation: refreshes keys if kid is not found in cache.
 * - Rate limiting: enforces a 30-second cooldown between refresh attempts to prevent DoS from invalid kid spam.
 * - Never trusts unverified keys.
 */
@Component
public class GoogleJwksKeyProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleJwksKeyProvider.class);

    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_COOLDOWN = Duration.ofSeconds(30);
    private static final Duration MAX_STALE_DURATION = Duration.ofHours(24);
    private static final int MAX_CACHE_KEYS = 50;
    private static final int MAX_RESPONSE_BYTES = 1024 * 1024; // 1 MB limit

    private final String jwksUri;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final Map<String, PublicKey> keyCache = new ConcurrentHashMap<>();
    private final AtomicReference<Instant> lastFetchTime = new AtomicReference<>(Instant.EPOCH);

    public GoogleJwksKeyProvider(
            @Value("${google.jwks-uri:https://www.googleapis.com/oauth2/v3/certs}") String jwksUri,
            ObjectMapper objectMapper) {
        this.jwksUri = jwksUri;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restTemplate = new RestTemplate(requestFactory);
        this.objectMapper = objectMapper;
    }

    // Secondary constructor for testing / manual wiring
    public GoogleJwksKeyProvider(String jwksUri, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.jwksUri = jwksUri;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Resolves an RSA public key by Google key ID (kid).
     *
     * @param kid Key ID from JWT header
     * @return PublicKey or null if not found
     */
    public PublicKey getPublicKey(String kid) {
        if (kid == null || kid.isBlank()) {
            return null;
        }

        Instant now = Instant.now();
        Instant lastFetch = lastFetchTime.get();
        boolean cacheExpired = Duration.between(lastFetch, now).compareTo(CACHE_TTL) > 0;

        PublicKey cachedKey = keyCache.get(kid);
        if (cachedKey != null && !cacheExpired) {
            return cachedKey;
        }

        // Key missing or cache expired: check cooldown to prevent rate limit / DoS
        if (Duration.between(lastFetch, now).compareTo(REFRESH_COOLDOWN) >= 0) {
            try {
                refreshKeys();
            } catch (GoogleServiceUnavailableException e) {
                // If refresh fails but key is in cache and within max acceptable stale duration (24h), use stale key
                if (cachedKey != null && Duration.between(lastFetch, now).compareTo(MAX_STALE_DURATION) <= 0) {
                    log.warn("Failed to refresh Google JWKS; falling back to stale key for kid={}", kid);
                    return cachedKey;
                }
                throw e;
            }
        }

        PublicKey result = keyCache.get(kid);
        if (result != null && Duration.between(lastFetchTime.get(), now).compareTo(MAX_STALE_DURATION) > 0) {
            throw new GoogleServiceUnavailableException("Google JWKS key for kid " + kid + " is older than 24 hours and could not be refreshed");
        }
        return result;
    }

    /**
     * Fetches JWKS from Google endpoint and updates the key cache.
     * Synchronized with double-checked cooldown verification to prevent concurrent bursts.
     */
    public synchronized void refreshKeys() {
        Instant now = Instant.now();
        // Double-check inside synchronized block: if refreshed concurrently within cooldown and cache not empty, skip
        if (!keyCache.isEmpty() && Duration.between(lastFetchTime.get(), now).compareTo(REFRESH_COOLDOWN) < 0) {
            log.debug("Skipping JWKS refresh; recently updated within cooldown");
            return;
        }

        log.info("Refreshing Google JWKS keys from endpoint: {}", jwksUri);
        try {
            String jwksJson = restTemplate.getForObject(jwksUri, String.class);
            if (jwksJson == null || jwksJson.isBlank()) {
                throw new GoogleServiceUnavailableException("Google JWKS response was empty");
            }

            if (jwksJson.length() > MAX_RESPONSE_BYTES) {
                log.error("Google JWKS response exceeded max allowed size of {} bytes", MAX_RESPONSE_BYTES);
                throw new GoogleServiceUnavailableException("Google JWKS response exceeded maximum allowed size");
            }

            JsonNode root = objectMapper.readTree(jwksJson);
            JsonNode keys = root.path("keys");
            if (!keys.isArray() || keys.isEmpty()) {
                throw new GoogleServiceUnavailableException("Google JWKS contained no keys");
            }

            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            for (JsonNode keyNode : keys) {
                if (keyCache.size() >= MAX_CACHE_KEYS) {
                    log.warn("JWKS key cache reached maximum capacity of {} keys; ignoring remaining keys", MAX_CACHE_KEYS);
                    break;
                }
                String kty = keyNode.path("kty").asText();
                String kid = keyNode.path("kid").asText();
                String n = keyNode.path("n").asText();
                String e = keyNode.path("e").asText();

                if ("RSA".equalsIgnoreCase(kty) && !kid.isBlank() && !n.isBlank() && !e.isBlank()) {
                    try {
                        byte[] nBytes = Base64.getUrlDecoder().decode(n);
                        byte[] eBytes = Base64.getUrlDecoder().decode(e);
                        BigInteger modulus = new BigInteger(1, nBytes);
                        BigInteger exponent = new BigInteger(1, eBytes);

                        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
                        PublicKey publicKey = keyFactory.generatePublic(spec);
                        keyCache.put(kid, publicKey);
                    } catch (IllegalArgumentException | InvalidKeySpecException ex) {
                        log.warn("Failed to parse Google JWKS key kid={}", kid, ex);
                    }
                }
            }

            lastFetchTime.set(Instant.now());
            log.info("Successfully refreshed Google JWKS keys. Total active keys in cache: {}", keyCache.size());
        } catch (GoogleServiceUnavailableException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.error("Network error while fetching Google JWKS: {}", ex.getMessage());
            throw new GoogleServiceUnavailableException("Unable to contact Google JWKS endpoint: " + ex.getMessage(), ex);
        } catch (Exception ex) {
            log.error("Error processing Google JWKS response: {}", ex.getMessage());
            throw new GoogleServiceUnavailableException("Failed to parse Google JWKS keys: " + ex.getMessage(), ex);
        }
    }

    /**
     * Registers a key directly into cache (useful for testing and offline mocking).
     */
    public void registerKey(String kid, PublicKey publicKey) {
        keyCache.put(kid, publicKey);
        lastFetchTime.set(Instant.now());
    }

    public void resetCooldown() {
        lastFetchTime.set(Instant.EPOCH);
    }

    /**
     * Clears cached keys (for testing key rotation).
     */
    public void clearCache() {
        keyCache.clear();
        lastFetchTime.set(Instant.EPOCH);
    }

    public int getCachedKeyCount() {
        return keyCache.size();
    }
}
