package com.wissensalt.springsecuritysessionredis.component;

import static org.apache.commons.collections4.CollectionUtils.emptyIfNull;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

import com.wissensalt.springsecuritysessionredis.model.Account;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class CustomAuthenticationManager implements AuthenticationManager {

  private final UserDetailsService userDetailsService;
  private final PasswordEncoder passwordEncoder;

  @Transactional(readOnly = true)
  @Override
  public Authentication authenticate(Authentication authentication) throws AuthenticationException {
    final Account account = (Account) userDetailsService
        .loadUserByUsername((String) authentication.getPrincipal());
    if (!passwordEncoder.matches((CharSequence) authentication.getCredentials(),
        account.getPassword())) {
      throw new BadCredentialsException("Wrong password");
    }

    final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
    if (isNotEmpty(account.getRoles())) {
      account.getRoles().forEach(role -> {
        authorities.add(new SimpleGrantedAuthority(role.getAuthority()));
        authorities.addAll(emptyIfNull(role.getPrivileges())
            .stream()
            .map(privilege -> new SimpleGrantedAuthority(privilege.getName()))
            .toList());
      });
    }

    return new UsernamePasswordAuthenticationToken(
        authentication.getPrincipal(),
        authentication.getCredentials(),
        authorities);
  }
}
