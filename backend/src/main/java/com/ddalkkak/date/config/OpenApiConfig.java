package com.ddalkkak.date.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// Swagger UI(/swagger-ui.html)와 OpenAPI 명세(/v3/api-docs)의 기본 정보.
// 운영(prod)에서는 application-prod.yml에서 springdoc을 꺼서 노출하지 않는다
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI openApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("데이트 딸깍 API")
                .description(
                    """
                    AI 기반 데이트 코스 추천 서비스 "데이트 딸깍"의 백엔드 API입니다.

                    - 모든 응답은 `ApiResponse`(`success` / `data` / `error`) 형식입니다.
                    - JSON 필드명은 snake_case입니다.
                    - 비로그인(게스트)은 `guest_id` 쿠키로 식별합니다.
                    """)
                .version("v1"))
        // 컨트롤러 경로에는 /api/v1 접두사가 WebConfig에서 붙으므로 서버 URL에는 넣지 않는다
        .addServersItem(new Server().url("/").description("현재 서버"));
  }

  // springdoc(swagger-core)은 Jackson 2로 스키마를 만들어서 spring.jackson.property-naming-strategy(SNAKE_CASE)를
  // 따르지 않는다. 실제 JSON과 문서가 어긋나지 않도록 스키마 속성명을 snake_case로 맞춘다
  @Bean
  public OpenApiCustomizer snakeCaseSchemaCustomizer() {
    return openApi -> {
      if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
        return;
      }
      openApi.getComponents().getSchemas().values().forEach(OpenApiConfig::renameToSnakeCase);
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static void renameToSnakeCase(Schema<?> schema) {
    Map<String, Schema> properties = schema.getProperties();
    if (properties != null) {
      Map<String, Schema> renamed = new LinkedHashMap<>();
      properties.forEach((name, property) -> renamed.put(toSnakeCase(name), property));
      schema.setProperties(renamed);
    }
    if (schema.getRequired() != null) {
      schema.setRequired(
          schema.getRequired().stream()
              .map(OpenApiConfig::toSnakeCase)
              .collect(Collectors.toList()));
    }
  }

  static String toSnakeCase(String name) {
    return name.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
  }
}
