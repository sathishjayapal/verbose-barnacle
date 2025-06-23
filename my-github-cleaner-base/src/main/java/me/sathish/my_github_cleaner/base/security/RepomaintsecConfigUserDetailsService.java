package me.sathish.my_github_cleaner.base.security;

import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class RepomaintsecConfigUserDetailsService implements UserDetailsService {

    @Override
    public UserDetails loadUserByUsername(final String username) {
        if ("admin".equals(username)) {
            final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(UserRoles.ADMIN));
            return User.withUsername(username)
                    .password("{bcrypt}$2a$10$FMzmOkkfbApEWxS.4XzCKOR7EbbiwzkPEyGgYh6uQiPxurkpzRMa6")
                    .authorities(authorities)
                    .build();
        } else if ("viewer".equals(username)) {
            final List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(UserRoles.VIEWER));
            return User.withUsername(username)
                    .password("{bcrypt}$2a$10$FMzmOkkfbApEWxS.4XzCKOR7EbbiwzkPEyGgYh6uQiPxurkpzRMa6")
                    .authorities(authorities)
                    .build();
        }
        throw new UsernameNotFoundException("User " + username + " not found");
    }
}
