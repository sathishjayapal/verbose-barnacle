package me.sathish.my_github_cleaner.base.config;

import static org.springframework.security.config.Customizer.withDefaults;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class RepomaintsecConfigSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // creates hashes with {bcrypt} prefix
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(final AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain basicFilterChain(final HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**")
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/api/auth/login")
                        .permitAll()
                        .anyRequest()
                        .hasAnyAuthority("VIEWER", "ADMIN"))
                .httpBasic(basic -> basic.realmName("basic realm"))
                .exceptionHandling(exception -> exception.authenticationEntryPoint((request, response, ex) -> {
                    // Basic-auth rejects unauthenticated requests in the filter
                    // chain, before the error-handling starter can format them —
                    // emit the same JSON shape ({code: AUTHORIZATION_DENIED})
                    // it would produce for AuthorizationDeniedException.
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"code\":\"AUTHORIZATION_DENIED\",\"message\":\"Unauthorized\"}");
                }))
                .build();
    }
    //    @Bean
    //    public SecurityFilterChain repomaintsecConfigFilterChain(final HttpSecurity http) throws
    //            Exception {
    //        return http.cors(withDefaults())
    //                .csrf(csrf -> csrf.ignoringRequestMatchers("/home", "/api/**", "/actuator/**"))
    //                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
    //                .formLogin(form ->
    //                        form.loginPage("/login").usernameParameter("login").failureUrl("/login?loginError=true"))
    //                .logout(logout -> logout.logoutSuccessUrl("/?logoutSuccess=true")
    //                        .deleteCookies("SESSION"))
    //                .csrf(csrf -> csrf.disable()).exceptionHandling(exception -> exception.authenticationEntryPoint(
    //                        new LoginUrlAuthenticationEntryPoint("/login?loginRequired=true")))
    //                .build();
    //    }

}
