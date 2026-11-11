package com.example.tournament.config;

import com.example.tournament.security.ApiSecurityHandlers;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Deux chaînes de sécurité sur le même backend métier :
 * <ul>
 *   <li>/api/** : HTTP Basic, sans session ni CSRF, erreurs 401/403 en JSON ;</li>
 *   <li>le reste (Thymeleaf) : formulaire de connexion, session et protection CSRF.</li>
 * </ul>
 * Matrice des droits (les rôles ne sont pas hiérarchiques : chaque règle liste ses rôles) :
 * <pre>
 * ADMIN                       : configuration du classement, suppression d'un tournoi
 * ADMIN, MANAGER              : gestion métier (tournois, équipes, inscriptions, calendrier)
 * ADMIN, MANAGER, OPERATOR    : opérations quotidiennes (saisie des résultats, démarrage d'un tournoi)
 * tous les rôles authentifiés : consultation
 * </pre>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final String ADMIN = "ADMIN";
    private static final String MANAGER = "MANAGER";
    private static final String OPERATOR = "OPERATOR";

    private final ApiSecurityHandlers apiSecurityHandlers;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    public SecurityFilterChain apiFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**")
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.authenticationEntryPoint(apiSecurityHandlers))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(apiSecurityHandlers)
                        .accessDeniedHandler(apiSecurityHandlers))
                .authorizeHttpRequests(auth -> auth
                        // ADMIN
                        .requestMatchers(HttpMethod.PUT, "/api/tournaments/*/ranking-config").hasRole(ADMIN)
                        .requestMatchers(HttpMethod.DELETE, "/api/tournaments/*").hasRole(ADMIN)
                        // OPERATOR et au-dessus
                        .requestMatchers(HttpMethod.PUT, "/api/matches/*/result").hasAnyRole(ADMIN, MANAGER, OPERATOR)
                        .requestMatchers(HttpMethod.POST, "/api/tournaments/*/start").hasAnyRole(ADMIN, MANAGER, OPERATOR)
                        // MANAGER et au-dessus : toute autre écriture
                        .requestMatchers(HttpMethod.GET, "/api/**").authenticated()
                        .requestMatchers("/api/**").hasAnyRole(ADMIN, MANAGER)
                        .anyRequest().authenticated());
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/js/**", "/error", "/error/**").permitAll()
                        // ADMIN
                        .requestMatchers("/tournaments/*/delete", "/tournaments/*/ranking-config").hasRole(ADMIN)
                        // OPERATOR et au-dessus
                        .requestMatchers("/matches/*/result", "/tournaments/*/start")
                        .hasAnyRole(ADMIN, MANAGER, OPERATOR)
                        // MANAGER et au-dessus : formulaires d'écriture et toute requête POST
                        .requestMatchers("/tournaments/new", "/tournaments/*/edit", "/tournaments/*/generate",
                                "/teams/new", "/teams/*/edit", "/matches/*/reschedule").hasAnyRole(ADMIN, MANAGER)
                        .requestMatchers(HttpMethod.POST, "/**").hasAnyRole(ADMIN, MANAGER)
                        // consultation
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                .exceptionHandling(e -> e.accessDeniedPage("/error/403"));
        return http.build();
    }
}
