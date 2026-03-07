package com.hittrivia.app.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Validated configuration properties for Apple Music MusicKit.
 * The app will fail to start if any of these are missing or malformed.
 */
@Validated
@ConfigurationProperties(prefix = "apple.music")
public record AppleMusicProperties(

        @NotBlank(message = "apple.music.team-id must be set (10-char Team ID from Apple Developer)")
        @Size(min = 10, max = 10, message = "apple.music.team-id must be exactly 10 characters")
        String teamId,

        @NotBlank(message = "apple.music.key-id must be set (10-char Key ID from Apple Developer)")
        @Size(min = 10, max = 10, message = "apple.music.key-id must be exactly 10 characters")
        String keyId,

        @NotBlank(message = "apple.music.private-key must be set (base64-encoded PKCS8 EC private key)")
        String privateKey
) {
}
