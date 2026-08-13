package com.smbility.railcargo.auth.jwt;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";
    private static final String CLAIM_TOKEN_TYPE = "tokenType";

    private final SecretKey key;
    private final JwtProperties jwtProperties;

    public JwtTokenProvider(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.key = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.expirationSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim(CLAIM_ROLE, member.getRole().name())
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public String createRefreshToken(Member member) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtProperties.refreshExpirationSeconds() * 1000);
        return Jwts.builder()
                .subject(String.valueOf(member.getId()))
                .claim(CLAIM_ROLE, member.getRole().name())
                .claim(CLAIM_TOKEN_TYPE, "refresh")
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    public LoginMember parseRefreshToken(String token) {
        Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        if (!"refresh".equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
            throw new JwtException("refresh token이 아닙니다.");
        }
        return new LoginMember(Long.valueOf(claims.getSubject()), MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class)));
    }

    public LoginMember parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long memberId = Long.valueOf(claims.getSubject());
        MemberRole role = MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class));
        return new LoginMember(memberId, role);
    }

    public boolean isValid(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
