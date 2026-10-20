package com.ddalkkak.date.service.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ddalkkak.date.domain.Region;
import com.ddalkkak.date.service.ai.LlmClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class PlaceCurationServiceTest {

  private static final PlaceBasicInfo PLACE =
      new PlaceBasicInfo(
          "1234",
          "연남동 와인바",
          "음식점 > 술집 > 와인바",
          "서울 마포구 연남동 1-1",
          null,
          null,
          null,
          Region.YEONNAM,
          true);

  private static final String VALID_JSON =
      """
      {"date_score": 8, "mood_tags": ["#조용한", "#와인", "로맨틱"],
       "price_range": "40,000-50,000원", "best_time": "저녁 19:00~22:00",
       "recommendation": "와인 한 잔과 대화를 나누기 좋은 분위기일 것 같아요."}
      """;

  // 호출된 프롬프트를 기록하고 준비된 응답을 차례로 돌려주는 가짜 Claude
  private static class FakeClaude implements LlmClient {
    final List<String> answers;
    final List<String> userPrompts = new ArrayList<>();
    String systemPrompt;

    FakeClaude(String... answers) {
      this.answers = new ArrayList<>(List.of(answers));
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) {
      this.systemPrompt = systemPrompt;
      userPrompts.add(userPrompt);
      return answers.remove(0);
    }
  }

  @Test
  void 장소_이름_카테고리_위치를_프롬프트로_보내고_5가지_결과를_돌려준다() {
    FakeClaude claude = new FakeClaude(VALID_JSON);

    CurationResult curation = new PlaceCurationService(claude).curate(PLACE);

    assertThat(curation.dateScore()).isEqualTo(8);
    assertThat(curation.moodTags()).containsExactly("#조용한", "#와인", "#로맨틱");
    assertThat(curation.priceRange()).isEqualTo("40,000-50,000원");
    assertThat(curation.estimatedCost()).isEqualTo(45000);
    assertThat(curation.bestTime()).isEqualTo("저녁 19:00~22:00");
    assertThat(curation.recommendation()).contains("와인");
    assertThat(claude.userPrompts.get(0))
        .contains("연남동 와인바", "음식점 > 술집 > 와인바", "연남", "서울 마포구 연남동 1-1");
  }

  @Test
  void 시스템_프롬프트는_JSON만_출력하고_이름_카테고리_위치로_추론하도록_지시한다() {
    FakeClaude claude = new FakeClaude(VALID_JSON);

    new PlaceCurationService(claude).curate(PLACE);

    assertThat(claude.systemPrompt)
        .contains("이름, 카테고리, 위치")
        .contains("JSON")
        .contains("date_score", "mood_tags", "price_range", "best_time", "recommendation");
  }

  @Test
  void 코드_블록과_앞뒤_설명이_붙어도_JSON을_꺼낸다() {
    FakeClaude claude = new FakeClaude("결과입니다.\n```json\n" + VALID_JSON + "\n```\n");

    CurationResult curation = new PlaceCurationService(claude).curate(PLACE);

    assertThat(curation.dateScore()).isEqualTo(8);
  }

  @Test
  void 태그는_최대_3개이고_중복은_제거하며_해시를_붙인다() {
    String json =
        """
        {"date_score": 5, "mood_tags": ["#a", "a", " b ", "#c", "#d"],
         "price_range": "무료(0원)", "best_time": "낮", "recommendation": "무난해요"}
        """;

    CurationResult curation = new PlaceCurationService(new FakeClaude(json)).curate(PLACE);

    assertThat(curation.moodTags()).containsExactly("#a", "#b", "#c");
  }

  @Test
  void 첫_응답이_잘못되면_한_번_더_시도한다() {
    FakeClaude claude = new FakeClaude("JSON이 아닙니다", VALID_JSON);

    CurationResult curation = new PlaceCurationService(claude).curate(PLACE);

    assertThat(curation.dateScore()).isEqualTo(8);
    assertThat(claude.userPrompts).hasSize(2);
  }

  @Test
  void 점수가_범위를_벗어난_응답이_계속되면_예외를_던진다() {
    String outOfRange =
        """
        {"date_score": 11, "mood_tags": ["#a"], "price_range": "1,000원",
         "best_time": "낮", "recommendation": "좋아요"}
        """;
    FakeClaude claude = new FakeClaude(outOfRange, outOfRange);

    assertThatThrownBy(() -> new PlaceCurationService(claude).curate(PLACE))
        .isInstanceOf(PlaceCurationException.class)
        .hasMessageContaining("연남동 와인바")
        .hasMessageContaining("date_score");
    assertThat(claude.userPrompts).hasSize(2);
  }

  @Test
  void 필수_값이_없으면_예외를_던진다() {
    String missing = "{\"date_score\": 7, \"mood_tags\": [\"#a\"]}";

    assertThatThrownBy(() -> PlaceCurationService.parseAiResponse(missing))
        .isInstanceOf(PlaceCurationException.class);
  }

  @Test
  void 가격대에_금액이_없어도_문구_그대로_받아들이고_비용은_0원이다() {
    String noAmount =
        """
        {"date_score": 7, "mood_tags": ["#a"], "price_range": "무료",
         "best_time": "낮", "recommendation": "좋아요"}
        """;

    CurationResult result = PlaceCurationService.parseAiResponse(noAmount);

    assertThat(result.priceRange()).isEqualTo("무료");
    assertThat(result.estimatedCost()).isZero();
  }

  @Test
  void 가격대가_비어_있으면_예외를_던진다() {
    String blank =
        """
        {"date_score": 7, "mood_tags": ["#a"], "price_range": " ",
         "best_time": "낮", "recommendation": "좋아요"}
        """;

    assertThatThrownBy(() -> PlaceCurationService.parseAiResponse(blank))
        .isInstanceOf(PlaceCurationException.class)
        .hasMessageContaining("price_range");
  }

  @Test
  void 가격대_문자열에서_중간_금액을_계산한다() {
    assertThat(result("30,000-50,000원").estimatedCost()).isEqualTo(40000);
    assertThat(result("약 15,000원").estimatedCost()).isEqualTo(15000);
    assertThat(result("1만원 이하").estimatedCost()).isEqualTo(1);
    assertThat(result("무료").estimatedCost()).isZero();
  }

  private static CurationResult result(String priceRange) {
    return new CurationResult(5, List.of("#a"), priceRange, "낮", "좋아요");
  }
}
