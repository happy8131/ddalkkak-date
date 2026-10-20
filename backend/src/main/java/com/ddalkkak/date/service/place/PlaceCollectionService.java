package com.ddalkkak.date.service.place;

import com.ddalkkak.date.domain.Place;
import com.ddalkkak.date.domain.Region;
import com.ddalkkak.date.repository.PlaceRepository;
import com.ddalkkak.date.service.ai.LlmRateLimitException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

// 서울 5대 상권별로 장소를 수집(카카오 Local API)하고 AI 큐레이션을 붙인다. 주 1회 갱신 배치도 같은 흐름이다
@Service
public class PlaceCollectionService {

  private static final Logger log = LoggerFactory.getLogger(PlaceCollectionService.class);

  static final int TARGET_PER_REGION = 100;
  private static final int SEARCH_RADIUS_METERS = 1500;
  // 카테고리가 한쪽으로 쏠리지 않도록 질의를 번갈아 가며 채운다
  private static final List<String> QUERY_SUFFIXES =
      List.of(
          "맛집", "카페", "술집", "디저트", "전시", "미술관", "공방", "방탈출", "보드게임", "산책", "레스토랑", "공원");

  private record Center(double longitude, double latitude) {}

  // 각 상권의 대표 지하철역 좌표
  private static final Map<Region, Center> CENTERS =
      Map.of(
          Region.HONGDAE, new Center(126.9236, 37.5572),
          Region.GANGNAM, new Center(127.0276, 37.4979),
          Region.SEONGSU, new Center(127.0557, 37.5446),
          Region.YEONNAM, new Center(126.9250, 37.5625),
          Region.ITAEWON, new Center(126.9942, 37.5345));

  public record RegionResult(Region region, int collected, int created, int updated) {}

  public record CurationBatchResult(int succeeded, int failed, int remaining) {}

  private final KakaoPlaceClient kakaoPlaceClient;
  private final PlaceCurationService curationService;
  private final PlaceRepository placeRepository;
  private final TransactionTemplate transactionTemplate;

  public PlaceCollectionService(
      KakaoPlaceClient kakaoPlaceClient,
      PlaceCurationService curationService,
      PlaceRepository placeRepository,
      TransactionTemplate transactionTemplate) {
    this.kakaoPlaceClient = kakaoPlaceClient;
    this.curationService = curationService;
    this.placeRepository = placeRepository;
    this.transactionTemplate = transactionTemplate;
  }

  // 한 상권에서 최대 100곳을 모아 places에 반영한다 (kakao_place_id 기준 upsert)
  public RegionResult collectRegion(Region region) {
    List<PlaceBasicInfo> places = searchRegion(region);
    int[] counts = transactionTemplate.execute(status -> upsert(places));
    return new RegionResult(region, places.size(), counts[0], counts[1]);
  }

  List<PlaceBasicInfo> searchRegion(Region region) {
    Center center = CENTERS.get(region);
    List<List<PlaceBasicInfo>> perQuery = new ArrayList<>();
    for (String suffix : QUERY_SUFFIXES) {
      perQuery.add(
          kakaoPlaceClient.search(
              region.label() + " " + suffix,
              region,
              center.longitude(),
              center.latitude(),
              SEARCH_RADIUS_METERS));
    }
    return interleave(perQuery, TARGET_PER_REGION);
  }

  // 질의 결과를 한 개씩 번갈아 꺼내 중복(kakao_place_id)을 빼고 limit개까지 채운다
  static List<PlaceBasicInfo> interleave(List<List<PlaceBasicInfo>> lists, int limit) {
    Map<String, PlaceBasicInfo> picked = new LinkedHashMap<>();
    int maxSize = lists.stream().mapToInt(List::size).max().orElse(0);
    for (int i = 0; i < maxSize && picked.size() < limit; i++) {
      for (List<PlaceBasicInfo> list : lists) {
        if (i < list.size() && picked.size() < limit) {
          picked.putIfAbsent(list.get(i).kakaoPlaceId(), list.get(i));
        }
      }
    }
    return List.copyOf(picked.values());
  }

  // [생성 수, 갱신 수]
  private int[] upsert(List<PlaceBasicInfo> infos) {
    LocalDateTime now = LocalDateTime.now();
    Map<String, Place> existing =
        placeRepository
            .findByKakaoPlaceIdIn(infos.stream().map(PlaceBasicInfo::kakaoPlaceId).toList())
            .stream()
            .collect(Collectors.toMap(Place::getKakaoPlaceId, Function.identity()));
    int created = 0;
    int updated = 0;
    List<Place> toSave = new ArrayList<>();
    for (PlaceBasicInfo info : infos) {
      Place place = existing.get(info.kakaoPlaceId());
      if (place == null) {
        toSave.add(Place.from(info, now));
        created++;
      } else {
        place.updateBasicInfo(info, now);
        updated++;
      }
    }
    placeRepository.saveAll(toSave);
    return new int[] {created, updated};
  }

  // 아직 AI 큐레이션이 없는 장소만 처리한다. 한 곳이 실패해도 나머지는 계속한다
  public CurationBatchResult curateUncurated() {
    List<Long> ids =
        placeRepository.findByCuratedAtIsNullAndActiveTrueOrderByIdAsc().stream()
            .map(Place::getId)
            .toList();
    int succeeded = 0;
    int failed = 0;
    for (int i = 0; i < ids.size(); i++) {
      Long id = ids.get(i);
      try {
        curate(id);
        succeeded++;
      } catch (LlmRateLimitException e) {
        // 호출 한도에 걸리면 나머지는 다음 실행에서 이어서 처리한다 (실패로 세지 않는다)
        int remaining = ids.size() - i;
        log.warn("LLM 호출 한도에 도달해 큐레이션을 중단합니다. 남은 장소 {}곳은 다음 실행에서 처리합니다.", remaining);
        return new CurationBatchResult(succeeded, failed, remaining);
      } catch (RuntimeException e) {
        failed++;
        log.warn("장소 큐레이션 실패 (place id={}): {}", id, e.getMessage());
      }
    }
    return new CurationBatchResult(succeeded, failed, 0);
  }

  // AI 호출은 트랜잭션 밖에서 하고, 결과 저장만 짧은 트랜잭션으로 묶는다
  private void curate(Long id) {
    Place place = placeRepository.findById(id).orElseThrow();
    CurationResult curation = curationService.curate(place.toBasicInfo());
    transactionTemplate.executeWithoutResult(
        status -> {
          Place managed = placeRepository.findById(id).orElseThrow();
          managed.applyCuration(curation, LocalDateTime.now());
        });
  }
}
