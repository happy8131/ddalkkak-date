package com.ddalkkak.date.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

  static final String API_PREFIX = "/api/v1";

  private final String[] allowedOrigins;

  public WebConfig(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
    this.allowedOrigins = allowedOrigins;
  }

  // controller 패키지의 모든 컨트롤러에 /api/v1 접두사를 붙인다
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.addPathPrefix(
        API_PREFIX, HandlerTypePredicate.forBasePackage("com.ddalkkak.date.controller"));
  }

  @Override
  public void addCorsMappings(CorsRegistry registry) {
    registry
        .addMapping(API_PREFIX + "/**")
        .allowedOrigins(allowedOrigins)
        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        .allowedHeaders("*")
        .allowCredentials(true); // guest_id HttpOnly 쿠키 전달용
  }
}
