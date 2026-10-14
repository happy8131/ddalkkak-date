package com.ddalkkak.date;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 실제 HTTP 서버(랜덤 포트) + 실제 PostgreSQL로 /api/v1 요청을 end-to-end로 검증한다 (Docker 필요)
// 업무 API가 아직 없어서 test 소스의 SampleController를 대상으로 한다
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiSmokeTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

  @LocalServerPort int port;

  private final HttpClient client = HttpClient.newHttpClient();

  private HttpResponse<String> send(HttpRequest.Builder builder) throws Exception {
    return client.send(builder.build(), BodyHandlers.ofString());
  }

  private HttpRequest.Builder request(String path) {
    return HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
  }

  @Test
  void GET_요청이_공통_응답_형식으로_반환된다() throws Exception {
    HttpResponse<String> res = send(request("/api/v1/sample/ok").GET());

    assertThat(res.statusCode()).isEqualTo(200);
    assertThat(res.headers().firstValue("Content-Type")).hasValueSatisfying(
        v -> assertThat(v).contains("application/json"));
    assertThat(res.body()).contains("\"success\":true").contains("\"data\":\"pong\"");
    assertThat(res.body()).doesNotContain("\"error\"");
  }

  @Test
  void POST_요청이_본문을_처리한다() throws Exception {
    HttpResponse<String> res =
        send(
            request("/api/v1/sample/echo")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\"name\":\"성수\",\"count\":2}")));

    assertThat(res.statusCode()).isEqualTo(200);
    assertThat(res.body()).contains("\"data\":\"성수:2\"");
  }

  @Test
  void 검증_실패는_400과_공통_에러_형식이다() throws Exception {
    HttpResponse<String> res =
        send(
            request("/api/v1/sample/echo")
                .header("Content-Type", "application/json")
                .POST(BodyPublishers.ofString("{\"name\":\"\",\"count\":0}")));

    assertThat(res.statusCode()).isEqualTo(400);
    assertThat(res.body())
        .contains("\"success\":false")
        .contains("\"code\":\"INVALID_REQUEST\"");
  }

  @Test
  void 없는_경로와_허용되지_않는_메서드는_공통_에러_형식이다() throws Exception {
    HttpResponse<String> notFound = send(request("/api/v1/nothing").GET());
    assertThat(notFound.statusCode()).isEqualTo(404);
    assertThat(notFound.body()).contains("\"code\":\"NOT_FOUND\"");

    HttpResponse<String> notAllowed = send(request("/api/v1/sample/ok").DELETE());
    assertThat(notAllowed.statusCode()).isEqualTo(405);
    assertThat(notAllowed.body()).contains("\"success\":false");
  }

  @Test
  void 서버_오류는_내부_정보_없이_500이다() throws Exception {
    HttpResponse<String> res = send(request("/api/v1/sample/unexpected-error").GET());

    assertThat(res.statusCode()).isEqualTo(500);
    assertThat(res.body()).contains("\"code\":\"INTERNAL_ERROR\"").doesNotContain("boom");
  }

  @Test
  void CORS_프리플라이트가_실제_서버에서_동작한다() throws Exception {
    HttpRequest.Builder allowed =
        request("/api/v1/sample/echo")
            .header("Origin", "http://localhost:3000")
            .header("Access-Control-Request-Method", "POST")
            .header("Access-Control-Request-Headers", "content-type")
            .method("OPTIONS", BodyPublishers.noBody());
    HttpResponse<String> ok = send(allowed);
    assertThat(ok.statusCode()).isEqualTo(200);
    assertThat(ok.headers().firstValue("Access-Control-Allow-Origin"))
        .hasValue("http://localhost:3000");
    assertThat(ok.headers().firstValue("Access-Control-Allow-Credentials")).hasValue("true");

    HttpRequest.Builder denied =
        request("/api/v1/sample/echo")
            .header("Origin", "http://evil.example.com")
            .header("Access-Control-Request-Method", "POST")
            .method("OPTIONS", BodyPublishers.noBody());
    assertThat(send(denied).statusCode()).isEqualTo(403);
  }
}
