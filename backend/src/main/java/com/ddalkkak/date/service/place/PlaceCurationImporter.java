package com.ddalkkak.date.service.place;

import com.ddalkkak.date.domain.Place;
import com.ddalkkak.date.repository.PlaceRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// 가격/소요시간을 수작업으로 큐레이션한 CSV를 places에 반영한다.
// 헤더: kakao_place_id,price_range,estimated_time[,is_alcohol]  (is_alcohol이 비면 기존 값 유지)
@Service
public class PlaceCurationImporter {

  private static final List<String> REQUIRED_HEADER =
      List.of("kakao_place_id", "price_range", "estimated_time");

  public record ImportResult(int updated, List<String> errors) {}

  private final PlaceRepository placeRepository;

  public PlaceCurationImporter(PlaceRepository placeRepository) {
    this.placeRepository = placeRepository;
  }

  @Transactional
  public ImportResult importCsv(Path file) throws IOException {
    List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
    if (lines.isEmpty()) {
      return new ImportResult(0, List.of("빈 파일입니다."));
    }
    List<String> header = splitLine(stripBom(lines.get(0)));
    if (header.size() < REQUIRED_HEADER.size()
        || !header.subList(0, REQUIRED_HEADER.size()).equals(REQUIRED_HEADER)) {
      return new ImportResult(
          0, List.of("헤더가 올바르지 않습니다. 필요한 헤더: " + String.join(",", REQUIRED_HEADER)));
    }
    boolean hasAlcohol = header.size() > REQUIRED_HEADER.size() && "is_alcohol".equals(header.get(3));

    int updated = 0;
    List<String> errors = new ArrayList<>();
    for (int i = 1; i < lines.size(); i++) {
      if (lines.get(i).isBlank()) {
        continue;
      }
      try {
        if (applyLine(splitLine(lines.get(i)), hasAlcohol)) {
          updated++;
        } else {
          errors.add("%d행: 수집되지 않은 kakao_place_id입니다.".formatted(i + 1));
        }
      } catch (IllegalArgumentException e) {
        errors.add("%d행: %s".formatted(i + 1, e.getMessage()));
      }
    }
    return new ImportResult(updated, errors);
  }

  private boolean applyLine(List<String> cells, boolean hasAlcohol) {
    if (cells.size() < REQUIRED_HEADER.size()) {
      throw new IllegalArgumentException("컬럼 수가 부족합니다.");
    }
    int priceRange;
    try {
      priceRange = Integer.parseInt(cells.get(1));
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("price_range가 숫자가 아닙니다: " + cells.get(1));
    }
    if (priceRange < 0) {
      throw new IllegalArgumentException("price_range는 0 이상이어야 합니다.");
    }
    String estimatedTime = cells.get(2);
    if (estimatedTime.isBlank() || estimatedTime.length() > 20) {
      throw new IllegalArgumentException("estimated_time은 1~20자여야 합니다.");
    }
    Boolean alcohol = null;
    if (hasAlcohol && cells.size() > 3 && !cells.get(3).isBlank()) {
      alcohol = parseBoolean(cells.get(3));
    }
    Optional<Place> place = placeRepository.findByKakaoPlaceId(cells.get(0));
    Boolean alcoholValue = alcohol;
    place.ifPresent(p -> p.applyManualCuration(priceRange, estimatedTime, alcoholValue));
    return place.isPresent();
  }

  private static boolean parseBoolean(String value) {
    return switch (value.toLowerCase()) {
      case "true", "y", "1" -> true;
      case "false", "n", "0" -> false;
      default -> throw new IllegalArgumentException("is_alcohol은 true/false여야 합니다: " + value);
    };
  }

  private static List<String> splitLine(String line) {
    return java.util.Arrays.stream(line.split(",", -1)).map(String::strip).toList();
  }

  private static String stripBom(String line) {
    return line.startsWith("﻿") ? line.substring(1) : line;
  }
}
