package com.ddalkkak.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 실제 PostgreSQL로 코스 생성 요청과 코스가 저장되는지 검증한다 (Docker 필요)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class CourseGenerateApiTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

  @Autowired MockMvc mockMvc;
  @Autowired DataSource dataSource;

  private MvcResult generate(Cookie... cookies) throws Exception {
    var builder =
        post("/api/v1/courses/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"region\":\"홍대\",\"date_type\":\"문화데이트\",\"budget\":100000}");
    if (cookies.length > 0) {
      builder.cookie(cookies);
    }
    return mockMvc
        .perform(builder)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.courses.length()").value(3))
        .andReturn();
  }

  @Test
  void 비로그인_요청의_코스_생성_이력과_코스가_저장된다() throws Exception {
    MvcResult result = generate();
    String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    String guestId = setCookie.substring("guest_id=".length(), setCookie.indexOf(';'));
    String body = result.getResponse().getContentAsString();

    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Map<String, Object> request =
        jdbc.queryForMap("SELECT * FROM course_requests WHERE guest_id = ?", guestId);
    assertThat(request.get("user_id")).isNull();
    assertThat(request.get("region")).isEqualTo("홍대");
    assertThat(request.get("date_type")).isEqualTo("문화데이트");
    assertThat(request.get("budget")).isEqualTo(100000);
    assertThat(request.get("created_at")).isNotNull();
    assertThat(request.get("response_time_ms")).isNotNull();
    assertThat(body).contains("\"request_id\":\"" + request.get("id") + "\"");

    List<Map<String, Object>> courses =
        jdbc.queryForList(
            "SELECT id, total_cost, jsonb_array_length(places_json) AS place_count"
                + " FROM courses WHERE request_id = ?",
            request.get("id"));
    assertThat(courses).hasSize(3);
    for (Map<String, Object> course : courses) {
      assertThat(body).contains("\"course_id\":\"" + course.get("id") + "\"");
      assertThat((Integer) course.get("place_count")).isBetween(3, 4);
      assertThat((Integer) course.get("total_cost")).isBetween(95000, 100000);
    }
  }

  @Test
  void 같은_guest_id로_다시_요청하면_이력이_같은_게스트로_쌓인다() throws Exception {
    MvcResult first = generate();
    String setCookie = first.getResponse().getHeader(HttpHeaders.SET_COOKIE);
    String guestId = setCookie.substring("guest_id=".length(), setCookie.indexOf(';'));

    MvcResult second = generate(new Cookie("guest_id", guestId));
    assertThat(second.getResponse().getHeader(HttpHeaders.SET_COOKIE)).isNull();

    Integer count =
        new JdbcTemplate(dataSource)
            .queryForObject(
                "SELECT count(*) FROM course_requests WHERE guest_id = ?", Integer.class, guestId);
    assertThat(count).isEqualTo(2);
  }
}
