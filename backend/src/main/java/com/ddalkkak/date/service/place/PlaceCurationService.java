package com.ddalkkak.date.service.place;

import com.ddalkkak.date.service.ai.LlmClient;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import tools.jackson.databind.json.JsonMapper;

// AI 큐레이터: 카카오 API가 주지 않는 주관적 정보(데이트 적합도, 분위기 등)를 LLM으로 분석한다.
// 가게 정보를 실제로 조회하는 것이 아니라 이름·카테고리·위치 조합으로 추론한다.
@Service
public class PlaceCurationService {

  static final String SYSTEM_PROMPT =
      """
      당신은 서울 데이트 코스를 큐레이션하는 전문가입니다.
      주어진 장소의 이름, 카테고리, 위치(지역과 주소)를 조합해서 데이트 장소로서의 특징을 추론하세요.
      실제 메뉴, 후기, 영업시간처럼 입력에 없는 사실을 아는 것처럼 단정하지 말고, 이름·카테고리·위치에서 합리적으로 짐작되는 범위에서만 판단하세요.

      반드시 아래 JSON 객체 하나만 출력하세요. 설명, 코드 블록 표시, 다른 문장은 출력하지 않습니다.
      {
        "date_score": 1~10 사이의 정수 (데이트 적합도, 10이 가장 적합),
        "mood_tags": ["#분위기태그", ...] (최대 3개, 각각 '#'으로 시작하는 한국어 해시태그),
        "price_range": "1인당 예상 가격대 문자열" (예: "30,000-50,000원"),
        "best_time": "추천 시간대" (예: "저녁 18:00~21:00"),
        "recommendation": "한 줄 추천 이유" (한국어 한 문장)
      }
      """;

  private static final int MAX_ATTEMPTS = 2;
  private static final int MAX_TAGS = 3;
  private static final int MAX_LABEL_LENGTH = 50;
  private static final JsonMapper JSON = JsonMapper.builder().build();

  private final LlmClient llmClient;

  public PlaceCurationService(LlmClient llmClient) {
    this.llmClient = llmClient;
  }

  // 응답이 JSON이 아니거나 값이 규칙에 어긋나면 한 번 더 시도하고, 그래도 안 되면 예외를 던진다
  public CurationResult curate(PlaceBasicInfo place) {
    PlaceCurationException last = null;
    for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
      try {
        return analyzeWithAi(place);
      } catch (PlaceCurationException e) {
        last = e;
      }
    }
    throw new PlaceCurationException(
        "AI 큐레이션 결과를 해석하지 못했습니다: " + place.name() + " (" + last.getMessage() + ")", last);
  }

  private CurationResult analyzeWithAi(PlaceBasicInfo place) {
    String prompt = buildCurationPrompt(place);
    String aiResponse = llmClient.complete(SYSTEM_PROMPT, prompt);
    return parseAiResponse(aiResponse);
  }

  static String buildCurationPrompt(PlaceBasicInfo place) {
    return """
        장소 이름: %s
        카테고리: %s
        지역: %s
        주소: %s
        """
        .formatted(place.name(), place.category(), place.region().label(), place.address());
  }

  static CurationResult parseAiResponse(String aiResponse) {
    String json = extractJsonObject(aiResponse);
    RawCuration raw;
    try {
      raw = JSON.readValue(json, RawCuration.class);
    } catch (RuntimeException e) {
      throw new PlaceCurationException("JSON 파싱 실패", e);
    }
    if (raw.dateScore() == null || raw.dateScore() < 1 || raw.dateScore() > 10) {
      throw new PlaceCurationException("date_score는 1~10이어야 합니다");
    }
    // "무료"처럼 금액이 없는 가격대도 허용한다 (places.price_range에는 0원으로 저장)
    if (!StringUtils.hasText(raw.priceRange())) {
      throw new PlaceCurationException("price_range가 비어 있습니다");
    }
    if (!StringUtils.hasText(raw.bestTime()) || !StringUtils.hasText(raw.recommendation())) {
      throw new PlaceCurationException("best_time과 recommendation은 비어 있을 수 없습니다");
    }
    List<String> tags = normalizeTags(raw.moodTags());
    if (tags.isEmpty()) {
      throw new PlaceCurationException("mood_tags가 비어 있습니다");
    }
    return new CurationResult(
        raw.dateScore(),
        tags,
        truncate(raw.priceRange().strip(), MAX_LABEL_LENGTH),
        truncate(raw.bestTime().strip(), MAX_LABEL_LENGTH),
        raw.recommendation().strip().replaceAll("\\s*\\R\\s*", " "));
  }

  // 모델이 코드 블록이나 앞뒤 설명을 붙여도 첫 '{'부터 마지막 '}'까지만 JSON으로 본다
  private static String extractJsonObject(String answer) {
    if (answer == null) {
      throw new PlaceCurationException("응답이 비어 있습니다");
    }
    int start = answer.indexOf('{');
    int end = answer.lastIndexOf('}');
    if (start < 0 || end < start) {
      throw new PlaceCurationException("JSON 객체를 찾지 못했습니다");
    }
    return answer.substring(start, end + 1);
  }

  private static List<String> normalizeTags(List<String> tags) {
    if (tags == null) {
      return List.of();
    }
    return tags.stream()
        .filter(StringUtils::hasText)
        .map(tag -> tag.strip().replaceAll("\\s+", ""))
        .map(tag -> tag.startsWith("#") ? tag : "#" + tag)
        .filter(tag -> tag.length() > 1)
        .distinct()
        .limit(MAX_TAGS)
        .toList();
  }

  private static String truncate(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record RawCuration(
      @JsonProperty("date_score") Integer dateScore,
      @JsonProperty("mood_tags") List<String> moodTags,
      @JsonProperty("price_range") String priceRange,
      @JsonProperty("best_time") String bestTime,
      @JsonProperty("recommendation") String recommendation) {}
}
