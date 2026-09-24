package com.projectfaust.config;

import com.projectfaust.auth.FaustUserDetailsService;
import com.projectfaust.auth.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration for Project Faust — JWT-based stateless authentication
 * with a hierarchical role model.
 *
 * <p><b>Role hierarchy:</b> {@link FaustUserDetailsService} grants exactly ONE
 * authority per user — {@code ROLE_<UserRole>}. Without a {@link RoleHierarchy},
 * an {@code ADMIN} token would have only {@code ROLE_ADMIN} and fail
 * {@code @PreAuthorize("hasRole('VIEWER')")} checks with a 403, since
 * {@code hasRole} checks for an exact authority match.
 *
 * <p>The hierarchy below makes higher roles automatically inherit all
 * permissions of lower roles:</p>
 * <pre>
 *   SUPER_ADMIN > ADMIN > ANALYST > VIEWER
 * </pre>
 * <p>e.g. a {@code SUPER_ADMIN} token satisfies {@code hasRole('VIEWER')},
 * {@code hasRole('ANALYST')}, {@code hasRole('ADMIN')}, and
 * {@code hasRole('SUPER_ADMIN')} checks — exactly the behaviour expected
 * from an operator with the highest clearance.</p>
 *
 * <p><b>Auth flow:</b></p>
 * <ol>
 *   <li>Client posts credentials to {@code POST /api/v1/auth/login}.</li>
 *   <li>{@link com.projectfaust.auth.AuthService} validates via
 *       {@link AuthenticationManager} and returns a signed JWT.</li>
 *   <li>Client sends {@code Authorization: Bearer <token>} on every subsequent request.</li>
 *   <li>{@link JwtAuthFilter} validates the token and populates the security context.</li>
 *   <li>{@code @PreAuthorize} annotations enforce role-based access control,
 *       evaluated against the {@link RoleHierarchy}.</li>
 * </ol>
 *
 * @author Dimitri / Project Faust
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final FaustUserDetailsService userDetailsService;

    /**
     * Defines the role hierarchy: each role on the left implies all roles
     * listed after {@code >} on the right.
     *
     * <p>Spring Security's {@code RoleHierarchyImpl} syntax:
     * {@code "ROLE_A > ROLE_B"} means a principal with {@code ROLE_A} is
     * also treated as having {@code ROLE_B} for {@code hasRole()} checks.</p>
     *
     * <p>This bean is picked up automatically by
     * {@link #methodSecurityExpressionHandler(RoleHierarchy)} and applied
     * to all {@code @PreAuthorize} evaluations.</p>
     */
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_SUPER_ADMIN > ROLE_ADMIN
                ROLE_ADMIN > ROLE_ANALYST
                ROLE_ANALYST > ROLE_VIEWER
                """);
    }

    /**
     * Wires the {@link RoleHierarchy} into the method security expression
     * handler used by {@code @PreAuthorize}.
     *
     * <p>Without this bean, the {@link RoleHierarchy} bean above has no
     * effect on {@code @PreAuthorize("hasRole(...)")} evaluations —
     * {@code hasRole} would still check for exact authority matches.</p>
     */
    @Bean
    public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
            RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler =
                new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    /**
     * Configures the HTTP security filter chain with JWT authentication.
     *
     * <ul>
     *   <li>CSRF disabled — stateless REST API.</li>
     *   <li>Sessions stateless — JWT carries all auth state.</li>
     *   <li>Login endpoint and Swagger UI publicly accessible.</li>
     *   <li>All other endpoints require authentication.</li>
     *   <li>{@link JwtAuthFilter} runs before the standard username/password filter.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter,
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt password encoder — strength 10.
     * Increase to 12 for production if latency budget allows.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * DAO authentication provider backed by {@link FaustUserDetailsService}.
     * Validates passwords against BCrypt hashes stored in the database.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} bean for use in
     * {@link com.projectfaust.auth.AuthService}.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}