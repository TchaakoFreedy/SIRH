package com.fric.sirh.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration MVC uniquement pour les ressources statiques.
 *
 * ⚠️ IMPORTANT : NE PAS définir addCorsMappings ici.
 * Le CORS est géré EXCLUSIVEMENT par SecurityConfig.corsConfigurationSource().
 *
 * Avoir deux configs CORS (WebMvcConfigurer + Spring Security) provoque l'erreur :
 *   "When allowCredentials is true, allowedOrigins cannot contain the special value '*'"
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:uploads/")
                .setCachePeriod(0);
    }

    // ❌ addCorsMappings SUPPRIMÉ volontairement — voir commentaire ci-dessus.
}