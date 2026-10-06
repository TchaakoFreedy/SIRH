package com.fric.sirh.service;

import com.fric.sirh.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TwoFactorVerificationService {

    private final UserService userService;
    private final TwoFactorAuthService twoFactorAuthService;
    private final UserDetailsService userDetailsService;

    public boolean verifyTwoFactorAndAuthenticate(String userId, int otpCode) {
        // Vérifier le code OTP
        boolean isValid = twoFactorAuthService.verifyOTP(userId, otpCode);

        if (isValid) {
            // Authentifier l'utilisateur
            User user = userService.findById(userId);
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            userDetails,
                            null,
                            userDetails.getAuthorities()
                    );

            SecurityContextHolder.getContext().setAuthentication(authentication);
            return true;
        }

        return false;
    }
}