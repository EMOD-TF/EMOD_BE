package com.emod.emod.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    // JwtAuthFilter (핵심부분만)
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (!StringUtils.hasText(header)) {
            request.setAttribute("authError", "missing_authorization_header");
            chain.doFilter(request, response);
            return;
        }
        if (!header.startsWith("Bearer ")) {
            request.setAttribute("authError", "invalid_scheme_expected_Bearer");
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(7);
        try {
            Jws<Claims> jws = jwtProvider.parseClaims(token);
            Claims claims = jws.getBody();

            Object raw = claims.get("authId");
            if (raw == null) {
                request.setAttribute("authError", "missing_claim_authId");
                chain.doFilter(request, response);
                return;
            }
            Long authId = (raw instanceof Number) ? ((Number) raw).longValue()
                    : Long.valueOf(raw.toString());

            String deviceCode = claims.get("deviceCode", String.class);

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(authId, null, Collections.emptyList());
            authentication.setDetails(deviceCode);

            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            request.setAttribute("authError", "token_expired");
        } catch (SignatureException e) {
            request.setAttribute("authError", "bad_signature_or_secret");
        } catch (JwtException e) {
            request.setAttribute("authError", "jwt_parse_error:" + e.getClass().getSimpleName());
        } catch (Exception e) {
            request.setAttribute("authError", "unexpected:" + e.getClass().getSimpleName());
        }

        chain.doFilter(request, response);
    }

}
