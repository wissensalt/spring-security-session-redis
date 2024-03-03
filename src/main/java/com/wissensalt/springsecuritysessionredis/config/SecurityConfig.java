package com.wissensalt.springsecuritysessionredis.config;

import com.wissensalt.springsecuritysessionredis.component.CustomAuthenticationManager;
import com.wissensalt.springsecuritysessionredis.component.CustomLogoutHandler;
import com.wissensalt.springsecuritysessionredis.model.Role.RoleName;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.session.HttpSessionEventPublisher;


@RequiredArgsConstructor
@Configuration
@EnableWebSecurity(debug = true)
@EnableMethodSecurity(
    securedEnabled = true,
    jsr250Enabled = true,
    proxyTargetClass = true
)
public class SecurityConfig {

  private final CustomLogoutHandler customLogoutHandler;

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    return http
        .securityContext(securityContext -> securityContext
            .securityContextRepository(new DelegatingSecurityContextRepository(
                new RequestAttributeSecurityContextRepository(),
                new HttpSessionSecurityContextRepository())))
        .csrf(AbstractHttpConfigurer::disable)
        .authorizeHttpRequests(request ->
            request.requestMatchers("/login").permitAll()
                .requestMatchers("/register").permitAll()
                .requestMatchers("/admin").hasAuthority(RoleName.ADMIN.getValue())
                .requestMatchers("/user").authenticated()
                .requestMatchers("**").authenticated())
        .sessionManagement(
            session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                .invalidSessionUrl("/logout?expired")
                .maximumSessions(1)
                .maxSessionsPreventsLogin(true))
        .logout(logout -> logout
            .logoutUrl("/logout")
            .invalidateHttpSession(true)
            .logoutSuccessHandler(customLogoutHandler))
        .build();
  }

  @Bean
  public HttpSessionEventPublisher httpSessionEventPublisher() {
    return new HttpSessionEventPublisher();
  }

  @Bean
  public HttpSessionSecurityContextRepository httpSessionSecurityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationProvider authenticationProvider(PasswordEncoder passwordEncoder,
      UserDetailsService userDetailsService) {
    final DaoAuthenticationProvider daoAuthenticationProvider =
        new DaoAuthenticationProvider();
    daoAuthenticationProvider.setUserDetailsService(userDetailsService);
    daoAuthenticationProvider.setPasswordEncoder(passwordEncoder);

    return daoAuthenticationProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(
      CustomAuthenticationManager customAuthenticationManager
  ) {

    return customAuthenticationManager;
  }
}
