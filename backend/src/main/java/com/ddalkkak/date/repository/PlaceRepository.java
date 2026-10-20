package com.ddalkkak.date.repository;

import com.ddalkkak.date.domain.Place;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceRepository extends JpaRepository<Place, Long> {

  List<Place> findByKakaoPlaceIdIn(Collection<String> kakaoPlaceIds);

  Optional<Place> findByKakaoPlaceId(String kakaoPlaceId);

  // AI 큐레이션이 아직 안 된 장소
  List<Place> findByCuratedAtIsNullAndActiveTrueOrderByIdAsc();

  long countByRegion(String region);
}
