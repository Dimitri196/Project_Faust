package com.projectfaust.auth;

import com.projectfaust.user.User;
import com.projectfaust.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Spring Security {@link UserDetailsService} implementation backed by
 * {@link UserRepository}.
 *
 * <p>Loads operator accounts from the database during authentication.
 * Replaces the development-only {@code InMemoryUserDetailsManager}.</p>
 *
 * <p>Roles are converted to Spring Security authorities with the
 * {@code ROLE_} prefix — required for {@code hasRole('ANALYST')} checks
 * in {@code @PreAuthorize} annotations.</p>
 *
 * @author Dimitri / Project Faust
 */
@Service
@RequiredArgsConstructor
public class FaustUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Loads a user by their email address.
     *
     * @param email the operator's registered email (used as username).
     * @return a {@link UserDetails} instance with role authorities.
     * @throws UsernameNotFoundException if no account matches the given email.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "FAUST_AUTH: No account found for email: " + email));

        // Convert UserRole enum to ROLE_X authority for Spring Security
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + user.getRole().name())
        );

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .build();
    }
}