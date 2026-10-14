package com.ddalkkak.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 실제 PostgreSQL(Testcontainers)로 전체 컨텍스트와 Flyway 마이그레이션을 검증한다 (Docker 필요)
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class DdalkkakDateApplicationTests {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

  @Autowired MockMvc mockMvc;
  @Autowired DataSource dataSource;

  @Test
  void 헬스체크가_UP을_반환한다() throws Exception {
    mockMvc
        .perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void Flyway_마이그레이션으로_핵심_테이블이_생성된다() {
    JdbcTemplate jdbc = new JdbcTemplate(dataSource);
    Integer count =
        jdbc.queryForObject(
            "SELECT count(*) FROM information_schema.tables WHERE table_schema = 'public'"
                + " AND table_name IN ('users','places','place_reviews','course_requests',"
                + "'courses','saved_courses','feedbacks','place_feedbacks')",
            Integer.class);
    assertThat(count).isEqualTo(8);
  }
}
