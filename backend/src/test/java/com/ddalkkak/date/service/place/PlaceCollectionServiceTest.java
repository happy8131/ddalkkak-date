package com.ddalkkak.date.service.place;

import static org.assertj.core.api.Assertions.assertThat;

import com.ddalkkak.date.domain.Region;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class PlaceCollectionServiceTest {

  private static PlaceBasicInfo place(String id) {
    return new PlaceBasicInfo(id, "장소" + id, "음식점", "주소", null, null, null, Region.HONGDAE, false);
  }

  private static List<PlaceBasicInfo> places(String prefix, int count) {
    return IntStream.range(0, count).mapToObj(i -> place(prefix + i)).toList();
  }

  @Test
  void 질의_결과를_번갈아_꺼내_섞는다() {
    List<PlaceBasicInfo> result =
        PlaceCollectionService.interleave(List.of(places("a", 3), places("b", 3)), 100);

    assertThat(result).extracting(PlaceBasicInfo::kakaoPlaceId)
        .containsExactly("a0", "b0", "a1", "b1", "a2", "b2");
  }

  @Test
  void 같은_장소는_한_번만_담고_limit을_넘지_않는다() {
    List<PlaceBasicInfo> first = List.of(place("x"), place("a1"));
    List<PlaceBasicInfo> second = List.of(place("x"), place("b1"), place("b2"));

    List<PlaceBasicInfo> result = PlaceCollectionService.interleave(List.of(first, second), 3);

    assertThat(result).extracting(PlaceBasicInfo::kakaoPlaceId).containsExactly("x", "a1", "b1");
  }

  @Test
  void 질의가_충분하면_상권당_100곳을_채운다() {
    List<List<PlaceBasicInfo>> lists =
        List.of(places("a", 45), places("b", 45), places("c", 45));

    assertThat(PlaceCollectionService.interleave(lists, PlaceCollectionService.TARGET_PER_REGION))
        .hasSize(100);
  }
}
