package com.ddalkkak.date.domain;

import com.ddalkkak.date.service.place.PlaceBasicInfo;
import com.ddalkkak.date.service.place.CurationResult;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 카카오 Local API로 수집한 장소. 가격/소요시간은 AI 추정 후 수작업 큐레이션으로 덮어쓸 수 있다
@Entity
@Table(name = "places")
public class Place {

  private static final String DATA_SOURCE_KAKAO = "kakao";

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;

  private String region;

  private String category;

  @Column(columnDefinition = "text")
  private String address;

  private BigDecimal latitude;

  private BigDecimal longitude;

  // 1인당 예상 비용(원). 아직 큐레이션되지 않았으면 0
  private int priceRange;

  private String estimatedTime;

  @Column(unique = true)
  private String kakaoPlaceId;

  @Column(columnDefinition = "text")
  private String kakaoPlaceUrl;

  private BigDecimal trustScore;

  private String dataSource;

  @Column(name = "is_alcohol")
  private boolean alcohol;

  @Column(name = "is_active")
  private boolean active;

  private LocalDateTime lastUpdatedAt;

  private Integer dateScore;

  @JdbcTypeCode(SqlTypes.JSON)
  private List<String> moodTags;

  private String priceRangeLabel;

  private String bestTime;

  @Column(columnDefinition = "text")
  private String recommendation;

  private LocalDateTime curatedAt;

  @Column(name = "is_manual_curated")
  private boolean manualCurated;

  protected Place() {}

  public static Place from(PlaceBasicInfo info, LocalDateTime now) {
    Place place = new Place();
    place.kakaoPlaceId = info.kakaoPlaceId();
    place.dataSource = DATA_SOURCE_KAKAO;
    place.active = true;
    place.updateBasicInfo(info, now);
    return place;
  }

  // 주 1회 갱신 때 카카오 원본 정보만 최신화한다 (큐레이션 값은 건드리지 않는다)
  public void updateBasicInfo(PlaceBasicInfo info, LocalDateTime now) {
    this.name = info.name();
    this.region = info.region().label();
    this.category = info.category();
    this.address = info.address();
    this.latitude = info.latitude();
    this.longitude = info.longitude();
    this.kakaoPlaceUrl = info.placeUrl();
    this.active = true;
    this.lastUpdatedAt = now;
    if (!manualCurated) {
      this.alcohol = info.alcohol();
    }
  }

  // AI 큐레이션 결과 반영. 수작업 큐레이션된 값(가격/소요시간)은 덮어쓰지 않는다
  public void applyCuration(CurationResult curation, LocalDateTime now) {
    this.dateScore = curation.dateScore();
    this.moodTags = List.copyOf(curation.moodTags());
    this.bestTime = curation.bestTime();
    this.recommendation = curation.recommendation();
    this.curatedAt = now;
    if (!manualCurated) {
      this.priceRange = curation.estimatedCost();
      this.priceRangeLabel = curation.priceRange();
    }
  }

  // import 도구로 들어온 수작업 큐레이션. alcohol이 null이면 기존 값을 유지한다
  public void applyManualCuration(int priceRange, String estimatedTime, Boolean alcohol) {
    this.priceRange = priceRange;
    this.estimatedTime = estimatedTime;
    this.priceRangeLabel = null;
    if (alcohol != null) {
      this.alcohol = alcohol;
    }
    this.manualCurated = true;
  }

  public PlaceBasicInfo toBasicInfo() {
    return new PlaceBasicInfo(
        kakaoPlaceId,
        name,
        category,
        address,
        latitude,
        longitude,
        kakaoPlaceUrl,
        Region.fromLabel(region),
        alcohol);
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getRegion() {
    return region;
  }

  public String getCategory() {
    return category;
  }

  public String getAddress() {
    return address;
  }

  public int getPriceRange() {
    return priceRange;
  }

  public String getEstimatedTime() {
    return estimatedTime;
  }

  public String getKakaoPlaceId() {
    return kakaoPlaceId;
  }

  public boolean isAlcohol() {
    return alcohol;
  }

  public boolean isActive() {
    return active;
  }

  public Integer getDateScore() {
    return dateScore;
  }

  public List<String> getMoodTags() {
    return moodTags;
  }

  public String getPriceRangeLabel() {
    return priceRangeLabel;
  }

  public String getBestTime() {
    return bestTime;
  }

  public String getRecommendation() {
    return recommendation;
  }

  public LocalDateTime getCuratedAt() {
    return curatedAt;
  }

  public boolean isManualCurated() {
    return manualCurated;
  }
}
