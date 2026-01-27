package com.app.config;


import com.app.service.OAuth2LoginSuccessHandler;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    private final OAuth2LoginSuccessHandler successHandler;
    private final JwtUtil jwtUtil;
    public SecurityConfig(OAuth2LoginSuccessHandler successHandler, JwtUtil jwtUtil) {
        this.successHandler = successHandler;
        this.jwtUtil = jwtUtil;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> {})
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((req, res, authEx) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write("{\"authenticated\":false}");
                        })
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/oauth2/**",
                                "/api/session-check"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth -> oauth
                        .successHandler(successHandler)
                )
                // 🔥 ADD JWT FILTER
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil),
                        org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

}
