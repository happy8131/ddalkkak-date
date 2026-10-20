package com.ddalkkak.date.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

// 카카오 Local API 설정. 키가 비어 있어도 앱은 뜨고, 장소 수집 배치를 실행할 때만 검사한다
@ConfigurationProperties(prefix = "app.kakao")
public record KakaoProperties(String restApiKey) {}
