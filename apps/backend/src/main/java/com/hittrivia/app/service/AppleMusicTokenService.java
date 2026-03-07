package com.hittrivia.app.service;

import com.hittrivia.app.config.AppleMusicProperties;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.ECDSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

@Service
public class AppleMusicTokenService {

    private static final Logger log = LoggerFactory.getLogger(AppleMusicTokenService.class);

    // We'll generate tokens valid for 24 hours and cache them
    private static final long TOKEN_LIFETIME_SECONDS = 86_400L;

    private final AppleMusicProperties props;

    private String cachedToken;
    private Instant cachedTokenExpiry;

    public AppleMusicTokenService(AppleMusicProperties props) {
        this.props = props;
    }

    /**
     * Returns a valid Apple Music developer token (ES256-signed JWT).
     * Tokens are cached until close to expiry to avoid re-signing on every request.
     */
    public synchronized String getDeveloperToken() {
        if (cachedToken != null && cachedTokenExpiry != null
                && Instant.now().isBefore(cachedTokenExpiry.minusSeconds(300))) {
            return cachedToken;
        }

        try {
            cachedToken = generateToken();
            cachedTokenExpiry = Instant.now().plusSeconds(TOKEN_LIFETIME_SECONDS);
            return cachedToken;
        } catch (Exception e) {
            log.error("Failed to generate Apple Music developer token", e);
            throw new RuntimeException("Could not generate Apple Music developer token", e);
        }
    }

    private String generateToken() throws Exception {
        ECPrivateKey privateKey = loadPrivateKey();

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(TOKEN_LIFETIME_SECONDS);

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES256)
                .keyID(props.keyId())
                .build();

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(props.teamId())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(exp))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        signedJWT.sign(new ECDSASigner(privateKey));

        log.info("Generated Apple Music developer token (expires {}) (token {})", exp, signedJWT);
        return signedJWT.serialize();
    }

    private ECPrivateKey loadPrivateKey() throws Exception {
        byte[] keyBytes = Base64.getDecoder().decode(props.privateKey());
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("EC");
        return (ECPrivateKey) keyFactory.generatePrivate(keySpec);
    }
}
