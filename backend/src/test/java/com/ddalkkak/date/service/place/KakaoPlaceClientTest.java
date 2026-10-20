package com.ddalkkak.date.service.place;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ddalkkak.date.config.KakaoProperties;
import com.ddalkkak.date.domain.Region;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KakaoPlaceClientTest {

  private static final String PAGE1 =
      """
      {"meta": {"is_end": false, "total_count": 4},
       "documents": [
        {"id": "111", "place_name": "홍대 와인바", "category_name": "음식점 > 술집 > 와인바",
         "address_name": "서울 마포구 서교동 1", "road_address_name": "서울 마포구 와우산로 1",
         "place_url": "http://place.map.kakao.com/111", "x": "126.92", "y": "37.55", "phone": "02-1"},
        {"id": "222", "place_name": "홍대 카페", "category_name": "음식점 > 카페",
         "address_name": "서울 마포구 서교동 2", "road_address_name": "",
         "place_url": "http://place.map.kakao.com/222", "x": "126.93", "y": "37.56"}
       ]}
      """;

  private static final String PAGE2 =
      """
      {"meta": {"is_end": true},
       "documents": [
        {"id": "333", "place_name": "홍대 미술관", "category_name": "문화,예술 > 미술관",
         "address_name": "서울 마포구 서교동 3", "road_address_name": "서울 마포구 3길",
         "place_url": "http://place.map.kakao.com/333", "x": "126.94", "y": "37.57"}
       ]}
      """;

  private KakaoPlaceClient clientWith(MockRestServiceServer[] server, String apiKey) {
    RestClient.Builder builder = RestClient.builder().baseUrl("https://dapi.kakao.com");
    server[0] = MockRestServiceServer.bindTo(builder).build();
    return new KakaoPlaceClient(builder.build(), new KakaoProperties(apiKey));
  }

  @Test
  void 키워드_검색_결과를_페이지_끝까지_모아_PlaceBasicInfo로_바꾼다() {
    MockRestServiceServer[] server = new MockRestServiceServer[1];
    KakaoPlaceClient client = clientWith(server, "test-key");
    server[0]
        .expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/search/keyword.json")))
        .andExpect(header("Authorization", "KakaoAK test-key"))
        .andExpect(queryParam("page", "1"))
        .andExpect(queryParam("size", "15"))
        .andExpect(queryParam("radius", "1500"))
        .andRespond(withSuccess(PAGE1, MediaType.APPLICATION_JSON));
    server[0]
        .expect(requestTo(org.hamcrest.Matchers.containsString("/v2/local/search/keyword.json")))
        .andExpect(queryParam("page", "2"))
        .andRespond(withSuccess(PAGE2, MediaType.APPLICATION_JSON));

    List<PlaceBasicInfo> places = client.search("홍대 맛집", Region.HONGDAE, 126.9236, 37.5572, 1500);

    server[0].verify();
    assertThat(places).hasSize(3);
    PlaceBasicInfo wineBar = places.get(0);
    assertThat(wineBar.kakaoPlaceId()).isEqualTo("111");
    assertThat(wineBar.name()).isEqualTo("홍대 와인바");
    assertThat(wineBar.address()).isEqualTo("서울 마포구 와우산로 1");
    assertThat(wineBar.longitude()).isEqualByComparingTo("126.92");
    assertThat(wineBar.latitude()).isEqualByComparingTo("37.55");
    assertThat(wineBar.region()).isEqualTo(Region.HONGDAE);
    assertThat(wineBar.alcohol()).isTrue();
    // 도로명 주소가 비면 지번 주소를 쓰고, 주류 업종이 아니면 alcohol=false
    assertThat(places.get(1).address()).isEqualTo("서울 마포구 서교동 2");
    assertThat(places.get(1).alcohol()).isFalse();
  }

  @Test
  void 키가_비어_있으면_호출하지_않고_예외를_던진다() {
    MockRestServiceServer[] server = new MockRestServiceServer[1];
    KakaoPlaceClient client = clientWith(server, "");

    assertThatThrownBy(() -> client.search("홍대 맛집", Region.HONGDAE, 126.9, 37.5, 1500))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("KAKAO_REST_API_KEY");
  }
}
