package com.fric.sirh.config;

import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.ICredentialRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class TwoFactorAuthConfig {

    @Bean
    public GoogleAuthenticator googleAuthenticator() {
        GoogleAuthenticator googleAuthenticator = new GoogleAuthenticator();
        googleAuthenticator.setCredentialRepository(new ICredentialRepository() {
            @Override
            public String getSecretKey(String userName) {
                return null;
            }

            @Override
            public void saveUserCredentials(String userName, String secretKey, int validationCode, List<Integer> scratchCodes) {
                // Stockage des credentials si nécessaire
            }
        });
        return googleAuthenticator;
    }
}