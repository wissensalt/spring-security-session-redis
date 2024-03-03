package com.wissensalt.springsecuritysessionredis.service;

import com.wissensalt.springsecuritysessionredis.param.LoginParam;
import com.wissensalt.springsecuritysessionredis.request.LoginRequest;
import com.wissensalt.springsecuritysessionredis.request.RegisterRequest;
import com.wissensalt.springsecuritysessionredis.model.Account;
import com.wissensalt.springsecuritysessionredis.model.Role;
import com.wissensalt.springsecuritysessionredis.repository.AccountRepository;
import com.wissensalt.springsecuritysessionredis.repository.RoleRepository;
import com.wissensalt.springsecuritysessionredis.response.AccountResponse;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.hibernate.ObjectNotFoundException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Service
public class AccountServiceImpl implements AccountService {

  private final AccountRepository accountRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final SecurityContextRepository securityContextRepository;


  @Transactional
  @Override
  public Boolean register(RegisterRequest request) {
    final Optional<Role> roleOptional = roleRepository.findFirstByName(request.role());
    if (roleOptional.isEmpty()) {
      throw new ObjectNotFoundException(roleOptional, request.role().getValue());
    }

    final Account account = new Account();
    account.setEmail(request.email());
    account.setPassword(passwordEncoder.encode(request.password()));
    account.getRoles().add(roleOptional.get());
    accountRepository.save(account);

    return true;
  }

  @Transactional(readOnly = true)
  @Override
  public AccountResponse login(LoginParam loginParam) {
    final LoginRequest request = loginParam.loginRequest();
    final UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(request.email(), request.password());
    final Authentication authentication =
        authenticationManager.authenticate(usernamePasswordAuthenticationToken);
    SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
        .getContextHolderStrategy();
    SecurityContext context = securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authentication);
    securityContextHolderStrategy.setContext(context);
    securityContextRepository.saveContext(context, loginParam.request(), loginParam.response());

    return new AccountResponse(loginParam.request().getSession().getId());
  }
}
