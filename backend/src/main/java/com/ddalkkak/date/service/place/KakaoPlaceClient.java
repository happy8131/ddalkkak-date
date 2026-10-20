package com.ddalkkak.date.service.place;

import com.ddalkkak.date.config.KakaoProperties;
import com.ddalkkak.date.domain.Region;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

// 카카오 Local API 키워드 검색. 한 질의당 최대 45개(15개 × 3페이지)까지만 돌려준다
@Component
public class KakaoPlaceClient {

  private static final String BASE_URL = "https://dapi.kakao.com";
  private static final int PAGE_SIZE = 15;
  private static final int MAX_PAGE = 3;
  // 주류 업종 판별용 카카오 카테고리 키워드 (19세 확인 대상, PRD)
  private static final List<String> ALCOHOL_KEYWORDS = List.of("술집", "주점", "바(BAR)", "와인바");

  private final RestClient restClient;
  private final KakaoProperties properties;

  @Autowired
  public KakaoPlaceClient(KakaoProperties properties) {
    this(RestClient.builder().baseUrl(BASE_URL).build(), properties);
  }

  KakaoPlaceClient(RestClient restClient, KakaoProperties properties) {
    this.restClient = restClient;
    this.properties = properties;
  }

  // 중심 좌표 반경 안에서 키워드로 검색한다
  public List<PlaceBasicInfo> search(
      String query, Region region, double longitude, double latitude, int radiusMeters) {
    if (!StringUtils.hasText(properties.restApiKey())) {
      throw new IllegalStateException("KAKAO_REST_API_KEY가 설정되지 않았습니다.");
    }
    List<PlaceBasicInfo> result = new ArrayList<>();
    for (int page = 1; page <= MAX_PAGE; page++) {
      int currentPage = page;
      KeywordResponse response =
          restClient
              .get()
              .uri(
                  uri ->
                      uri.path("/v2/local/search/keyword.json")
                          .queryParam("query", query)
                          .queryParam("x", longitude)
                          .queryParam("y", latitude)
                          .queryParam("radius", radiusMeters)
                          .queryParam("page", currentPage)
                          .queryParam("size", PAGE_SIZE)
                          .build())
              .header("Authorization", "KakaoAK " + properties.restApiKey())
              .retrieve()
              .body(KeywordResponse.class);
      if (response == null || response.documents() == null) {
        break;
      }
      response.documents().stream().map(doc -> toBasicInfo(doc, region)).forEach(result::add);
      if (response.meta() == null || response.meta().isEnd()) {
        break;
      }
    }
    return result;
  }

  private static PlaceBasicInfo toBasicInfo(Document doc, Region region) {
    String category = doc.categoryName() == null ? "" : doc.categoryName();
    String address =
        StringUtils.hasText(doc.roadAddressName()) ? doc.roadAddressName() : doc.addressName();
    return new PlaceBasicInfo(
        doc.id(),
        truncate(doc.placeName(), 100),
        truncate(category, 50),
        address == null ? "" : address,
        parseDecimal(doc.y()),
        parseDecimal(doc.x()),
        doc.placeUrl(),
        region,
        ALCOHOL_KEYWORDS.stream().anyMatch(category::contains));
  }

  private static BigDecimal parseDecimal(String value) {
    return StringUtils.hasText(value) ? new BigDecimal(value) : null;
  }

  private static String truncate(String value, int max) {
    if (value == null) {
      return "";
    }
    return value.length() <= max ? value : value.substring(0, max);
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  record KeywordResponse(List<Document> documents, Meta meta) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Meta(@JsonProperty("is_end") boolean isEnd) {}

  @JsonIgnoreProperties(ignoreUnknown = true)
  record Document(
      String id,
      @JsonProperty("place_name") String placeName,
      @JsonProperty("category_name") String categoryName,
      @JsonProperty("address_name") String addressName,
      @JsonProperty("road_address_name") String roadAddressName,
      @JsonProperty("place_url") String placeUrl,
      String x,
      String y) {}
}
