package com.fanzzi.backend.common.infrastructure.config;

import com.fanzzi.backend.auth.jwt.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .cors(cors -> cors.configurationSource(request -> {

                    CorsConfiguration config = new CorsConfiguration();
                    config.setAllowCredentials(true);
                    config.setAllowedHeaders(List.of("*"));
                    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
                    config.setAllowedOriginPatterns(List.of("*"));

                    config.addAllowedHeader("*");
                    config.addAllowedMethod("*");

                    return config;
                }))

                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )


                .exceptionHandling(ex -> ex

                        // If token missing/invalid → 401
                        .authenticationEntryPoint((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"message\":\"Unauthorized\"}"
                            );
                        })

                        // If token valid but role not allowed → 403
                        .accessDeniedHandler((req, res, e) -> {
                            res.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            res.setContentType("application/json");
                            res.getWriter().write(
                                    "{\"message\":\"Forbidden\"}"
                            );
                        })
                )

                .authorizeHttpRequests(auth -> auth

                        .requestMatchers(
                                "/health",
                                "/public/**",
                                "/auth/user/**",
                                "/auth/admin/**",
                                "/ws/**"

                        ).permitAll()

                        .requestMatchers("/api/channels/**").hasRole("USER")
                        .requestMatchers("/api/media/**").hasRole("USER")
                        .requestMatchers("/api/user/**").hasRole("USER")
                        .requestMatchers("/api/stars/**").hasRole("USER")
                        .requestMatchers("/api/posts/**").hasRole("USER")
                        .requestMatchers("/api/comments/**").hasRole("USER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")


                        .anyRequest().authenticated()
                )

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return username -> {
            throw new UsernameNotFoundException("JWT only authentication");
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
