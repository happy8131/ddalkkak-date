package com.ddalkkak.date.batch;

import com.ddalkkak.date.domain.Region;
import com.ddalkkak.date.service.place.PlaceCollectionService;
import com.ddalkkak.date.service.place.PlaceCollectionService.CurationBatchResult;
import com.ddalkkak.date.service.place.PlaceCollectionService.RegionResult;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

// 장소 수집 배치. 주 1회 갱신용 ECS Scheduled Task가 이 러너를 켜서 실행한다
// (SPRING_PROFILES_ACTIVE=prod,batch + APP_BATCH_PLACE_COLLECTION_ENABLED=true)
@Component
@ConditionalOnProperty(prefix = "app.batch.place-collection", name = "enabled", havingValue = "true")
public class PlaceCollectionRunner implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(PlaceCollectionRunner.class);

  private final PlaceCollectionService collectionService;

  public PlaceCollectionRunner(PlaceCollectionService collectionService) {
    this.collectionService = collectionService;
  }

  @Override
  public void run(ApplicationArguments args) {
    List<Region> failedRegions = new ArrayList<>();
    for (Region region : Region.values()) {
      try {
        RegionResult result = collectionService.collectRegion(region);
        log.info(
            "장소 수집 완료: {} 수집 {}곳 (신규 {}, 갱신 {})",
            region.label(),
            result.collected(),
            result.created(),
            result.updated());
      } catch (RuntimeException e) {
        failedRegions.add(region);
        log.error("장소 수집 실패: {}", region.label(), e);
      }
    }

    CurationBatchResult curation = collectionService.curateUncurated();
    log.info(
        "AI 큐레이션 완료: 성공 {}곳, 실패 {}곳, 한도로 미처리 {}곳",
        curation.succeeded(),
        curation.failed(),
        curation.remaining());

    // 실패가 있으면 비정상 종료해서 ECS 태스크가 실패로 남게 한다
    if (!failedRegions.isEmpty() || curation.failed() > 0) {
      throw new IllegalStateException(
          "장소 수집 배치에 실패가 있습니다. 수집 실패 상권=%s, 큐레이션 실패=%d곳"
              .formatted(failedRegions, curation.failed()));
    }
  }
}
