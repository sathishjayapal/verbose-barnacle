package me.sathish.my_github_cleaner.base.security;

import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class RepomaintsecConfigUserDetailsService implements UserDetailsService {

    private final Environment environment;
    private final PasswordEncoder passwordEncoder;

    public RepomaintsecConfigUserDetailsService(Environment environment, PasswordEncoder passwordEncoder) {
        this.environment = environment;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(final String username) {
        String adminUsername = environment.getProperty("app.security.admin.username");
        String adminPassword = environment.getProperty("app.security.admin.password");
        String viewerUsername = environment.getProperty("app.security.viewer.username");
        String viewerPassword = environment.getProperty("app.security.viewer.password");

        if (adminUsername.equals(username)) {
            final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(UserRoles.ADMIN));
            return User.withUsername(username)
                    .password(adminPassword)
                    .authorities(authorities)
                    .build();
        } else if (viewerUsername.equals(username)) {
            final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(UserRoles.VIEWER));
            return User.withUsername(username)
                    .password(viewerPassword)
                    .authorities(authorities)
                    .build();
        }
        throw new UsernameNotFoundException("User " + username + " not found");
    }
}
