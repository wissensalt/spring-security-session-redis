package com.wissensalt.springsecuritysessionredis.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.BiFunction;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.MapSession;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.data.redis.RedisSessionMapper;
import org.springframework.session.data.redis.RedisSessionRepository;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.session.web.http.HeaderHttpSessionIdResolver;
import org.springframework.session.web.http.HttpSessionIdResolver;

@Configuration
@EnableRedisHttpSession
public class SessionConfig implements BeanClassLoaderAware {

  private ClassLoader loader;

  @Bean
  public LettuceConnectionFactory redisConnectionFactory() {
    final RedisStandaloneConfiguration redisStandaloneConfiguration = (RedisStandaloneConfiguration) LettuceConnectionFactory.createRedisConfiguration(
        RedisURI.builder()
            .withHost("localhost")
            .withPort(6380)
            .build());

    LettuceClientConfiguration lettuceClientConfiguration = LettuceClientConfiguration.builder()
        .commandTimeout(Duration.of(1, ChronoUnit.MINUTES))
        .build();

    return new LettuceConnectionFactory(redisStandaloneConfiguration, lettuceClientConfiguration);
  }

  @Bean
  public RedisSerializer<Object> springSessionDefaultRedisSerializer() {
    return new GenericJackson2JsonRedisSerializer(objectMapper());
  }

  private ObjectMapper objectMapper() {
    ObjectMapper mapper = new ObjectMapper();
    mapper.registerModules(SecurityJackson2Modules.getModules(this.loader));
    return mapper;
  }

  @Bean
  @Qualifier("httpSessionIdResolver")
  public HttpSessionIdResolver httpSessionIdResolver() {

    return HeaderHttpSessionIdResolver.xAuthToken();
  }

  @Override
  public void setBeanClassLoader(ClassLoader classLoader) {
    this.loader = classLoader;
  }

  @Bean
  public SessionRepositoryCustomizer<RedisSessionRepository> redisSessionRepositoryCustomizer() {
    return redisSessionRepository -> redisSessionRepository
        .setRedisSessionMapper(new SafeRedisSessionMapper(redisSessionRepository));
  }

  static class SafeRedisSessionMapper implements
      BiFunction<String, Map<String, Object>, MapSession> {

    private final RedisSessionMapper delegate = new RedisSessionMapper();

    private final RedisSessionRepository sessionRepository;

    SafeRedisSessionMapper(RedisSessionRepository sessionRepository) {
      this.sessionRepository = sessionRepository;
    }

    @Override
    public MapSession apply(String sessionId, Map<String, Object> map) {
      try {
        return this.delegate.apply(sessionId, map);
      } catch (IllegalStateException ex) {
        this.sessionRepository.deleteById(sessionId);
        return null;
      }
    }

  }
}
