package com.ddalkkak.date.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ddalkkak.date.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class OpenAiCompatibleClientTest {

  private static final String BASE_URL = "https://api.example.com/v1";

  private static LlmProperties properties(String apiKey) {
    return new LlmProperties(BASE_URL, apiKey, "test-model", 256, 0);
  }

  @Test
  void chat_completions로_요청하고_첫_선택지의_내용을_돌려준다() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), properties("test-key"));
    server
        .expect(requestTo(BASE_URL + "/chat/completions"))
        .andExpect(header("Authorization", "Bearer test-key"))
        .andExpect(jsonPath("$.model").value("test-model"))
        .andExpect(jsonPath("$.max_tokens").value(256))
        .andExpect(jsonPath("$.messages[0].role").value("system"))
        .andExpect(jsonPath("$.messages[0].content").value("시스템"))
        .andExpect(jsonPath("$.messages[1].role").value("user"))
        .andExpect(jsonPath("$.messages[1].content").value("질문"))
        .andRespond(
            withSuccess(
                """
                {"id": "c1", "choices": [{"index": 0, "finish_reason": "stop",
                  "message": {"role": "assistant", "content": "안녕하세요"}}],
                 "usage": {"total_tokens": 10}}
                """,
                MediaType.APPLICATION_JSON));

    assertThat(client.complete("시스템", "질문")).isEqualTo("안녕하세요");
    server.verify();
  }

  @Test
  void 짧은_Retry_After가_붙은_429는_기다렸다가_재시도한다() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), properties("test-key"));
    server
        .expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "1"));
    server
        .expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(
            withSuccess(
                "{\"choices\": [{\"message\": {\"role\": \"assistant\", \"content\": \"성공\"}}]}",
                MediaType.APPLICATION_JSON));

    assertThat(client.complete("s", "u")).isEqualTo("성공");
    server.verify();
  }

  @Test
  void Retry_After가_길면_재시도하지_않고_한도_예외를_던진다() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), properties("test-key"));
    server
        .expect(requestTo(BASE_URL + "/chat/completions"))
        .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS).header("Retry-After", "3600"));

    assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(LlmRateLimitException.class);
    server.verify();
  }

  @Test
  void 호출_한도_429는_LlmRateLimitException으로_바꾼다() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), properties("test-key"));
    server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

    assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(LlmRateLimitException.class);
  }

  @Test
  void 그_밖의_HTTP_오류는_IllegalStateException이다() {
    RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    OpenAiCompatibleClient client = new OpenAiCompatibleClient(builder.build(), properties("test-key"));
    server.expect(requestTo(BASE_URL + "/chat/completions")).andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThatThrownBy(() -> client.complete("s", "u")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  void 키가_비어_있으면_예외를_던진다() {
    OpenAiCompatibleClient client =
        new OpenAiCompatibleClient(RestClient.builder().build(), properties(""));

    assertThatThrownBy(() -> client.complete("s", "u"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("LLM_API_KEY");
  }
}
