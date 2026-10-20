package com.ddalkkak.date.service.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ddalkkak.date.config.AnthropicProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AnthropicClaudeClientTest {

  @Test
  void Messages_API로_요청하고_텍스트_블록을_이어서_돌려준다() {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://api.anthropic.com");
    MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    AnthropicClaudeClient client =
        new AnthropicClaudeClient(
            builder.build(), new AnthropicProperties("test-key", "claude-haiku-5-5", 512));
    server
        .expect(requestTo("https://api.anthropic.com/v1/messages"))
        .andExpect(header("x-api-key", "test-key"))
        .andExpect(header("anthropic-version", "2023-06-01"))
        .andExpect(jsonPath("$.model").value("claude-haiku-5-5"))
        .andExpect(jsonPath("$.max_tokens").value(512))
        .andExpect(jsonPath("$.system").value("시스템"))
        .andExpect(jsonPath("$.messages[0].role").value("user"))
        .andExpect(jsonPath("$.messages[0].content").value("질문"))
        .andRespond(
            withSuccess(
                """
                {"id": "msg_1", "type": "message", "role": "assistant",
                 "content": [{"type": "text", "text": "안녕"}, {"type": "text", "text": "하세요"}],
                 "stop_reason": "end_turn"}
                """,
                MediaType.APPLICATION_JSON));

    String answer = client.complete("시스템", "질문");

    server.verify();
    assertThat(answer).isEqualTo("안녕하세요");
  }

  @Test
  void 키가_비어_있으면_예외를_던진다() {
    AnthropicClaudeClient client =
        new AnthropicClaudeClient(
            RestClient.builder().build(), new AnthropicProperties("", "claude-haiku-5-5", 512));

    assertThatThrownBy(() -> client.complete("s", "u"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ANTHROPIC_API_KEY");
  }
}
