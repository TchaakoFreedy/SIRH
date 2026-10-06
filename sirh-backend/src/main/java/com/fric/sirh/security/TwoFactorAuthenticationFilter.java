package com.fric.sirh.security;

import com.fric.sirh.model.User;
import com.fric.sirh.service.TwoFactorAuthService;
import com.fric.sirh.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
public class TwoFactorAuthenticationFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;

    public TwoFactorAuthenticationFilter(
            AuthenticationManager authenticationManager,
            UserService userService,
            TwoFactorAuthService twoFactorAuthService) {
        this.authenticationManager = authenticationManager;
        this.userService = userService;
        this.twoFactorAuthService = twoFactorAuthService;
        setFilterProcessesUrl("/api/auth/login");
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
            throws AuthenticationException {
        String username = request.getParameter("email");
        String password = request.getParameter("password");

        // Première étape: authentification avec email/password
        UsernamePasswordAuthenticationToken authRequest =
                new UsernamePasswordAuthenticationToken(username, password);

        Authentication authentication = authenticationManager.authenticate(authRequest);

        // Vérifier si le 2FA est activé
        User user = userService.findByEmail(username);
        if (user.isTwoFactorEnabled()) {
            // Si 2FA activé, on stocke l'utilisateur en session pour la deuxième étape
            request.getSession().setAttribute("2FA_USER_ID", user.getId());
            // On ne complète pas l'authentification
            return null;
        }

        return authentication;
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response,
                                            FilterChain chain, Authentication authResult)
            throws IOException, ServletException {
        SecurityContextHolder.getContext().setAuthentication(authResult);
        response.setStatus(HttpServletResponse.SC_OK);
    }
}