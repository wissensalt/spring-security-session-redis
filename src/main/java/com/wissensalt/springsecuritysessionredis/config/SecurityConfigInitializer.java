package com.wissensalt.springsecuritysessionredis.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.context.AbstractHttpSessionApplicationInitializer;

@Configuration
public class SecurityConfigInitializer extends AbstractHttpSessionApplicationInitializer {

  public SecurityConfigInitializer() {
    super(SecurityConfig.class, SessionConfig.class);
  }
}
