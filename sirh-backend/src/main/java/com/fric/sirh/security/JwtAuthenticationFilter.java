package com.fric.sirh.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService customUserDetailsService;

    private static final List<String> PUBLIC_ENDPOINTS = Arrays.asList(
            "/api/auth/login",
            "/api/auth/register",
            "/api/auth/forgot-password",
            "/api/auth/reset-password",
            "/api/auth/refresh-token",
            "/api/users/login",
            "/api/2fa/pending",
            "/api/2fa/verify",
            "/api/2fa/verify-backup",
            "/api/2fa/status",
            "/swagger-ui",
            "/v3/api-docs",
            "/api/auth/verify-2fa",
            "/actuator/health",
            "/actuator/info",
            "/uploads/",
            "/ws/"
    );

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            log.debug("Requete OPTIONS ignoree");
            filterChain.doFilter(request, response);
            return;
        }

        String requestURI = request.getRequestURI();

        // Ignorer les requêtes WebSocket (soit par en-tête Upgrade, soit par URL)
        String upgradeHeader = request.getHeader("Upgrade");
        if ("websocket".equalsIgnoreCase(upgradeHeader) || requestURI.startsWith("/ws/")) {
            log.debug("Requete WebSocket ignoree (pas de token requis) : {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        if (isPublicEndpoint(requestURI)) {
            log.debug("Endpoint public: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = extractJwtFromRequest(request);

        log.debug("Requete: [{}] {} | Auth Header: {}, Token present: {}",
                request.getMethod(),
                requestURI,
                StringUtils.hasText(request.getHeader("Authorization")),
                StringUtils.hasText(jwt));

        if (!StringUtils.hasText(jwt)) {
            log.warn("Aucun token JWT trouve pour: {}", requestURI);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\": \"Authentication required\", \"message\": \"No JWT token provided\"}");
            return;
        }

        try {
            if (jwtService.isValid(jwt)) {
                String userId = jwtService.extractUserId(jwt);
                String role = jwtService.extractRole(jwt);
                List<String> permissions = jwtService.extractPermissions(jwt);

                log.info("Token valide - UserId: {}, Role: {}, Permissions: {}",
                        userId, role, permissions != null ? permissions.size() : 0);

                if (StringUtils.hasText(userId)) {
                    CustomUserDetails userDetails = (CustomUserDetails) customUserDetailsService.loadUserById(userId);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    null,
                                    userDetails.getAuthorities()
                            );

                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("Authentification reussie pour: {} (ID: {})",
                            userDetails.getUsername(), userId);
                } else {
                    log.warn("UserId extrait est null ou vide");
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Invalid token\", \"message\": \"User ID not found in token\"}");
                    return;
                }
            } else {
                log.warn("Token JWT invalide ou expire");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.getWriter().write("{\"error\": \"Invalid token\", \"message\": \"Token is invalid or expired\"}");
                return;
            }
        } catch (UsernameNotFoundException e) {
            log.error("Utilisateur non trouve pour ID: {}", e.getMessage());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"User not found\", \"message\": \"" + e.getMessage() + "\"}");
            return;
        } catch (Exception e) {
            log.error("Erreur authentication JWT: {}", e.getMessage(), e);
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Authentication error\", \"message\": \"" + e.getMessage() + "\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isPublicEndpoint(String uri) {
        for (String endpoint : PUBLIC_ENDPOINTS) {
            if (uri.startsWith(endpoint)) {
                return true;
            }
        }
        return false;
    }

    private String extractJwtFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String xAuthToken = request.getHeader("X-Auth-Token");
        if (StringUtils.hasText(xAuthToken)) {
            log.debug("Token trouve dans X-Auth-Token header");
            return xAuthToken;
        }

        String tokenParam = request.getParameter("token");
        if (StringUtils.hasText(tokenParam)) {
            log.warn("Token JWT trouve dans parametre URL (deprecie): {}",
                    request.getRequestURI());
            return tokenParam;
        }

        return null;
    }
}