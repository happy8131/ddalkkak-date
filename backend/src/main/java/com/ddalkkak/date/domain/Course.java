package com.ddalkkak.date.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

// 생성된 코스. 장소 목록은 places_json(JSONB)에 비정규화해서 저장한다
@Entity
@Table(name = "courses")
public class Course {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private String id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "request_id")
  private CourseRequest request;

  private String title;

  private int totalCost;

  private String totalTime;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "places_json")
  private List<CoursePlace> places;

  private LocalDateTime createdAt;

  protected Course() {}

  public Course(
      CourseRequest request,
      String title,
      List<CoursePlace> places,
      int totalCost,
      String totalTime) {
    this.request = request;
    this.title = title;
    this.places = List.copyOf(places);
    this.totalCost = totalCost;
    this.totalTime = totalTime;
    this.createdAt = request.getCreatedAt();
  }

  public String getId() {
    return id;
  }

  public CourseRequest getRequest() {
    return request;
  }

  public String getTitle() {
    return title;
  }

  public int getTotalCost() {
    return totalCost;
  }

  public String getTotalTime() {
    return totalTime;
  }

  public List<CoursePlace> getPlaces() {
    return places;
  }

  public LocalDateTime getCreatedAt() {
    return createdAt;
  }
}
