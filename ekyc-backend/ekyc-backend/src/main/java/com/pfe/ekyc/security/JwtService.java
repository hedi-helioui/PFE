package com.pfe.ekyc.security;

import com.pfe.ekyc.config.KycProperties;
import com.pfe.ekyc.domain.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Service
public class JwtService {

    private final KycProperties props;
    private SecretKey key;

    public JwtService(KycProperties props) {
        this.props = props;
    }

    @PostConstruct
    void init() {
        byte[] secret = props.jwt().secret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("kyc.jwt.secret doit contenir au moins 32 caractères");
        }
        this.key = Keys.hmacShaKeyFor(secret);
    }

    public long expiresInSeconds() {
        return Duration.ofMinutes(props.jwt().expirationMinutes()).toSeconds();
    }

    public String generate(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getEmail())
                .claim("uid", user.getId())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expiresInSeconds())))
                .signWith(key)
                .compact();
    }

    /** Valide la signature et l'expiration ; lève une JwtException si le jeton est invalide. */
    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
