package com.fric.sirh.config;

import com.fric.sirh.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS géré UNIQUEMENT ici, via le bean corsConfigurationSource()
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authz -> authz
                        // Autoriser les WebSockets en priorité
                        .requestMatchers(HttpMethod.GET, "/ws/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Endpoints publics
                        .requestMatchers("/api/users/login", "/api/auth/login", "/api/auth/**").permitAll()
                        .requestMatchers("/api/2fa/pending", "/api/2fa/verify", "/api/2fa/verify-backup", "/api/2fa/status/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers("/uploads/**", "/uploads/contrats/**").permitAll()
                        .requestMatchers("/api/pay-slips/upload").permitAll()

                        // Endpoints nécessitant authentification
                        .requestMatchers("/api/2fa/generate/**", "/api/2fa/admin/**").authenticated()
                        .requestMatchers("/api/pay-slips/**").authenticated()

                        // Toute autre requête doit être authentifiée
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ⚠️ CORRECTION : on utilise setAllowedOriginPatterns (compatible avec allowCredentials=true)
        // au lieu de setAllowedOrigins("*") qui était interdit.
        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:4200",
                "http://localhost:3000",
                "http://localhost:8080",
                "http://127.0.0.1:4200",
                "http://127.0.0.1:8080",
                "https://sirh-ytal.vercel.app",
                "https://*.vercel.app"
        ));

        configuration.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"
        ));

        configuration.setAllowedHeaders(Arrays.asList("*"));

        configuration.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Idempotency-Key",
                "X-Request-ID",
                "Content-Disposition",
                "X-Total-Count",
                "X-Page-Count"
        ));

        // Avec Authorization: Bearer, allowCredentials(true) n'est PAS nécessaire.
        // On le laisse à true uniquement si tu utilises des cookies.
        // Pour éviter tout conflit, on le passe à false ici.
        configuration.setAllowCredentials(false);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public BCryptPasswordEncoder bCryptPasswordEncoder() {
        return new BCryptPasswordEncoder();
    }
}