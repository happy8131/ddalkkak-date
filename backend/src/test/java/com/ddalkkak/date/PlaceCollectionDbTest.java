package com.ddalkkak.date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import com.ddalkkak.date.domain.Place;
import com.ddalkkak.date.domain.Region;
import com.ddalkkak.date.repository.PlaceRepository;
import com.ddalkkak.date.service.ai.LlmClient;
import com.ddalkkak.date.service.ai.LlmRateLimitException;
import com.ddalkkak.date.service.place.KakaoPlaceClient;
import com.ddalkkak.date.service.place.PlaceBasicInfo;
import com.ddalkkak.date.service.place.PlaceCollectionService;
import com.ddalkkak.date.service.place.PlaceCurationImporter;
import com.ddalkkak.date.service.place.PlaceCurationImporter.ImportResult;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

// 실제 PostgreSQL로 장소 수집(upsert) → AI 큐레이션 → 수작업 import 흐름을 검증한다 (Docker 필요).
// 카카오/LLM 호출은 가짜로 바꾼다.
@SpringBootTest
@Testcontainers
class PlaceCollectionDbTest {

  @Container @ServiceConnection
  static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:15");

  private static final String LLM_ANSWER =
      """
      {"date_score": 9, "mood_tags": ["#감성", "#조용한"], "price_range": "25,000-35,000원",
       "best_time": "저녁", "recommendation": "분위기가 좋을 것 같아요."}
      """;

  @MockitoBean KakaoPlaceClient kakaoPlaceClient;
  @MockitoBean LlmClient llmClient;

  @Autowired PlaceCollectionService collectionService;
  @Autowired PlaceCurationImporter importer;
  @Autowired PlaceRepository placeRepository;

  @BeforeEach
  void setUp() {
    placeRepository.deleteAll();
    when(llmClient.complete(anyString(), anyString())).thenReturn(LLM_ANSWER);
  }

  private static PlaceBasicInfo info(String id, String name, boolean alcohol) {
    return new PlaceBasicInfo(
        id,
        name,
        alcohol ? "음식점 > 술집" : "음식점 > 카페",
        "서울 마포구 " + id,
        new BigDecimal("37.55"),
        new BigDecimal("126.92"),
        "http://place.map.kakao.com/" + id,
        Region.HONGDAE,
        alcohol);
  }

  private void kakaoReturns(List<PlaceBasicInfo> places) {
    when(kakaoPlaceClient.search(anyString(), eq(Region.HONGDAE), anyDouble(), anyDouble(), anyInt()))
        .thenReturn(places);
    when(kakaoPlaceClient.search(anyString(), any(Region.class), anyDouble(), anyDouble(), anyInt()))
        .thenAnswer(
            invocation ->
                invocation.getArgument(1) == Region.HONGDAE ? places : List.<PlaceBasicInfo>of());
  }

  @Test
  void 수집을_다시_돌려도_중복_없이_기본_정보만_갱신한다() {
    kakaoReturns(List.of(info("p1", "홍대 카페", false), info("p2", "홍대 바", true)));

    var first = collectionService.collectRegion(Region.HONGDAE);
    kakaoReturns(List.of(info("p1", "홍대 카페(이전)", false), info("p3", "홍대 갤러리", false)));
    var second = collectionService.collectRegion(Region.HONGDAE);

    assertThat(first.created()).isEqualTo(2);
    assertThat(second.created()).isEqualTo(1);
    assertThat(second.updated()).isEqualTo(1);
    assertThat(placeRepository.countByRegion("홍대")).isEqualTo(3);
    Place renamed = placeRepository.findByKakaoPlaceId("p1").orElseThrow();
    assertThat(renamed.getName()).isEqualTo("홍대 카페(이전)");
    assertThat(placeRepository.findByKakaoPlaceId("p2").orElseThrow().isAlcohol()).isTrue();
  }

  @Test
  void 큐레이션되지_않은_장소만_AI로_분석해_저장한다() {
    kakaoReturns(List.of(info("p1", "홍대 카페", false), info("p2", "홍대 바", true)));
    collectionService.collectRegion(Region.HONGDAE);

    var first = collectionService.curateUncurated();
    var second = collectionService.curateUncurated();

    assertThat(first.succeeded()).isEqualTo(2);
    assertThat(first.failed()).isZero();
    assertThat(second.succeeded()).isZero();
    Place place = placeRepository.findByKakaoPlaceId("p1").orElseThrow();
    assertThat(place.getDateScore()).isEqualTo(9);
    assertThat(place.getMoodTags()).containsExactly("#감성", "#조용한");
    assertThat(place.getPriceRange()).isEqualTo(30000);
    assertThat(place.getPriceRangeLabel()).isEqualTo("25,000-35,000원");
    assertThat(place.getBestTime()).isEqualTo("저녁");
    assertThat(place.getRecommendation()).contains("분위기");
    assertThat(place.getCuratedAt()).isNotNull();
  }

  @Test
  void 큐레이션_실패한_장소가_있어도_나머지는_저장한다() {
    kakaoReturns(List.of(info("p1", "홍대 카페", false), info("p2", "홍대 바", true)));
    collectionService.collectRegion(Region.HONGDAE);
    when(llmClient.complete(anyString(), anyString()))
        .thenReturn("JSON 아님", "JSON 아님") // p1: 재시도까지 실패
        .thenReturn(LLM_ANSWER); // p2: 성공

    var result = collectionService.curateUncurated();

    assertThat(result.succeeded()).isEqualTo(1);
    assertThat(result.failed()).isEqualTo(1);
    assertThat(placeRepository.findByKakaoPlaceId("p1").orElseThrow().getDateScore()).isNull();
    assertThat(placeRepository.findByKakaoPlaceId("p2").orElseThrow().getDateScore()).isEqualTo(9);
  }

  @Test
  void 수작업_import한_가격과_소요시간은_AI_큐레이션과_재수집이_덮어쓰지_않는다(@TempDir Path dir)
      throws Exception {
    kakaoReturns(List.of(info("p1", "홍대 카페", false), info("unknown-skip", "다른 곳", false)));
    collectionService.collectRegion(Region.HONGDAE);
    Path csv = dir.resolve("curation.csv");
    Files.writeString(
        csv,
        "kakao_place_id,price_range,estimated_time,is_alcohol\n"
            + "p1,12000,1.5시간,true\n"
            + "nope,1000,1시간,\n"
            + "unknown-skip,abc,1시간,\n");

    ImportResult result = importer.importCsv(csv);
    collectionService.curateUncurated();
    kakaoReturns(List.of(info("p1", "홍대 카페", false)));
    collectionService.collectRegion(Region.HONGDAE);

    assertThat(result.updated()).isEqualTo(1);
    assertThat(result.errors()).hasSize(2);
    Place place = placeRepository.findByKakaoPlaceId("p1").orElseThrow();
    assertThat(place.isManualCurated()).isTrue();
    assertThat(place.getPriceRange()).isEqualTo(12000); // AI의 30000이 아님
    assertThat(place.getPriceRangeLabel()).isNull();
    assertThat(place.getEstimatedTime()).isEqualTo("1.5시간");
    assertThat(place.isAlcohol()).isTrue(); // 재수집이 false로 되돌리지 않음
    assertThat(place.getDateScore()).isEqualTo(9); // 분위기 등 AI 값은 채워짐
  }

  @Test
  void 잘못된_헤더의_CSV는_아무것도_반영하지_않는다(@TempDir Path dir) throws Exception {
    Path csv = dir.resolve("bad.csv");
    Files.writeString(csv, "id,price\np1,1000\n");

    ImportResult result = importer.importCsv(csv);

    assertThat(result.updated()).isZero();
    assertThat(result.errors()).singleElement().asString().contains("헤더");
  }

  @Test
  void 상권당_여러_질의_결과를_합쳐도_100곳을_넘지_않는다() {
    List<PlaceBasicInfo> many =
        IntStream.range(0, 150).mapToObj(i -> info("m" + i, "장소" + i, false)).toList();
    kakaoReturns(many);

    var result = collectionService.collectRegion(Region.HONGDAE);

    assertThat(result.collected()).isEqualTo(100);
    assertThat(placeRepository.countByRegion("홍대")).isEqualTo(100);
  }

  @Test
  void 호출_한도에_걸리면_실패로_세지_않고_멈춘다_다음_실행에서_이어간다() {
    kakaoReturns(List.of(info("p1", "홍대 카페", false), info("p2", "홍대 바", true)));
    collectionService.collectRegion(Region.HONGDAE);
    when(llmClient.complete(anyString(), anyString()))
        .thenReturn(LLM_ANSWER)
        .thenThrow(new LlmRateLimitException("한도", null));

    var first = collectionService.curateUncurated();
    doReturn(LLM_ANSWER).when(llmClient).complete(anyString(), anyString());
    var second = collectionService.curateUncurated();

    assertThat(first.succeeded()).isEqualTo(1);
    assertThat(first.failed()).isZero();
    assertThat(first.remaining()).isEqualTo(1);
    assertThat(second.succeeded()).isEqualTo(1);
    assertThat(second.remaining()).isZero();
    assertThat(placeRepository.findByCuratedAtIsNullAndActiveTrueOrderByIdAsc()).isEmpty();
  }
}
