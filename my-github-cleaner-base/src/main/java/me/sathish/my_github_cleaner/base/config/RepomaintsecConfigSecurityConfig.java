package me.sathish.my_github_cleaner.base.config;

import static org.springframework.security.config.Customizer.withDefaults;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;



@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class RepomaintsecConfigSecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        // creates hashes with {bcrypt} prefix
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
@Bean
    public SecurityFilterChain basicFilterChain(final HttpSecurity http) throws Exception {
        return http.securityMatcher("/api/**")
                .cors(withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize.anyRequest().hasAuthority("VIEWER"))
                .httpBasic(basic -> basic.realmName("basic realm"))
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