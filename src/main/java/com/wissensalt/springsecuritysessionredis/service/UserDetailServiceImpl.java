package com.wissensalt.springsecuritysessionredis.service;

import com.wissensalt.springsecuritysessionredis.model.Account;
import com.wissensalt.springsecuritysessionredis.repository.AccountRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class UserDetailServiceImpl implements UserDetailsService {

  private final AccountRepository accountRepository;

  @Transactional(readOnly = true)
  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    final Optional<Account> userOptional = accountRepository.findFirstByEmail(username);
    if (userOptional.isEmpty()) {
      throw new UsernameNotFoundException(String.format("User with email %s Not Found", username));
    }

    return userOptional.get();
  }
}
