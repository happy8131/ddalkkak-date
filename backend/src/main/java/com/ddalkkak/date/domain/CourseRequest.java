package com.ddalkkak.date.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;

// 코스 생성 요청 이력. 비로그인이면 userId는 null이고 guestId로 식별한다
@Entity
@Table(name = "course_requests")
public class CourseRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private Long userId;

  private String guestId;

  private String region;

  private String dateType;

  private int budget;

  private LocalDateTime createdAt;

  private Integer responseTimeMs;

  protected CourseRequest() {}

  public CourseRequest(
      Long userId,
      String guestId,
      CourseCondition condition,
      LocalDateTime createdAt,
      int responseTimeMs) {
    this.userId = userId;
    this.guestId = guestId;
    this.region = condition.region().label();
    this.dateType = condition.dateType().label();
    this.budget = condition.budget();
    this.createdAt = createdAt;
    this.responseTimeMs = responseTimeMs;
  }

  public UUID getId() {
    return id;
  }

  public Long getUserId() {
    return userId;
  }

  public String getGuestId() {
    return guestId;
  }

  public String getRegion() {
    return region;
  }

  public String getDateType() {
    return dateType;
  }

  public int getBudget() {
    return budget;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }

  public Integer getResponseTimeMs() {
    return responseTimeMs;
  }
}
