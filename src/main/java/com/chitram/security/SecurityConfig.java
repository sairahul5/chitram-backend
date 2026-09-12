package com.chitram.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/health", "/error", "/oauth2/**", "/login/**", "/api/auth/logout",
                                "/api/auth/session")
                        .permitAll()
                        .requestMatchers("/ws/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/visual-items/**", "/api/recommendations/status")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/user/search", "/api/user/profile/*").permitAll()
                        .requestMatchers("/api/admin/**", "/api/user/**").authenticated()
                        .anyRequest().authenticated())
                .cors(Customizer.withDefaults())
                .exceptionHandling(exception -> exception
                        .defaultAuthenticationEntryPointFor(
                                apiAuthenticationEntryPoint(),
                                apiRequestMatcher()))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"))
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("CHITRAM_SESSION")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(jakarta.servlet.http.HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true}");
                        }))
                .oauth2Login(oauth2 -> oauth2.successHandler(oAuth2LoginSuccessHandler));

        return http.build();
    }

    @Bean
    AuthenticationEntryPoint apiAuthenticationEntryPoint() {
        return new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
    }

    @Bean
    RequestMatcher apiRequestMatcher() {
        return request -> request.getServletPath().startsWith("/api/");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${FRONTEND_URL:http://localhost:3000}") String frontendUrl) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of(frontendUrl.split(","))
                .stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setExposedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }
}
