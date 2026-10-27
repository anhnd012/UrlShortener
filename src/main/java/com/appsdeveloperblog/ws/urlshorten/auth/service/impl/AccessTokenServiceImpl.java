package com.appsdeveloperblog.ws.urlshorten.auth.service.impl;

import com.appsdeveloperblog.ws.urlshorten.auth.service.AccessTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class AccessTokenServiceImpl implements AccessTokenService {

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.audience}")
    private String audience;

    @Value("${app.jwt.acc_token_expiration}")
    private Long accessTokenExpiration;

    private final SecretKey key;
    private final JwtParser parser;

    public AccessTokenServiceImpl(@Value("${app.jwt.secret-base64}") String secretBase64) {
        key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(secretBase64)
        );

        parser = Jwts.parser()
                .verifyWith(key)
                .sig().clear().add(Jwts.SIG.HS256).and()
                .requireIssuer(issuer)
                .requireAudience(audience)
                .require("token_use", "access")
                .build();
    }

    @Override
    public String create(UUID userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                .issuer(issuer)
                .subject(userId.toString())
                .audience().add(audience).and()
                .claim("uid", userId.toString())
                .claim("token_use", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTokenExpiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    @Override
    public Claims verify(String rawToken) {
        Claims claims = parser.parseSignedClaims(rawToken).getBody();

        if(claims.get("uid") == null
        || claims.get("token_use") == null
        || claims.getExpiration() == null){
            throw new JwtException("Missing required claims: uid or token_use or expiration claim is missing");
        }


        UUID.fromString(claims.getSubject());

        return claims;
    }
}
