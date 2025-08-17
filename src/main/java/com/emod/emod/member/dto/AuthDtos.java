package com.emod.emod.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

public class AuthDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SignupRequest {
        @NotBlank
        private String deviceCode; // 최초 시작하기 클릭 시 받는 코드
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SignupResponse {
        private Long authId;
        private String deviceCode;
        private boolean profileCompleted; // profile 테이블에 추가정보가 이미 있는지
        private String jwt;
    }
}
