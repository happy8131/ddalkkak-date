package com.ddalkkak.date.service;

import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

// 비로그인 사용자를 식별하는 guest_id 쿠키 (PRD FR-1.3, NFR 보안: HttpOnly, Secure, SameSite=Lax)
@Component
public class GuestIdCookie {

  public static final String NAME = "guest_id";

  static final Duration MAX_AGE = Duration.ofDays(365);

  // 요청에 유효한 guest_id가 있으면 그대로 쓰고, 없거나 형식이 틀리면 새로 발급해 응답에 싣는다
  public String resolve(String cookieValue, HttpServletResponse response) {
    if (isValid(cookieValue)) {
      return cookieValue;
    }
    String guestId = UUID.randomUUID().toString();
    ResponseCookie cookie =
        ResponseCookie.from(NAME, guestId)
            .httpOnly(true)
            .secure(true)
            .sameSite("Lax")
            .path("/")
            .maxAge(MAX_AGE)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    return guestId;
  }

  private static boolean isValid(String value) {
    if (value == null) {
      return false;
    }
    try {
      return UUID.fromString(value).toString().equals(value);
    } catch (IllegalArgumentException e) {
      return false;
    }
  }
}
