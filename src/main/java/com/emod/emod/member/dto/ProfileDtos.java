package com.emod.emod.member.dto;

import com.emod.emod.domain.AttentionSpan;
import com.emod.emod.domain.EmotionExpression;
import com.emod.emod.domain.Gender;
import com.emod.emod.domain.LearningPlace;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

public class ProfileDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpsertRequest {
        @NotBlank
        private String name;
        @NotNull
        @Min(1900)
        @Max(2100)
        private Integer birthYear;
        @NotNull
        @Min(1)
        @Max(12)
        private Integer birthMonth;
        @NotNull
        private Gender gender;

        // ✅ 프론트는 한국어 라벨 문자열로 보냄 → Enum으로 매핑됨
        @NotNull
        private EmotionExpression q1; // "거의 표현하지 못해요" | "짧은..." | "상황에 맞춰..."
        @NotNull
        private AttentionSpan q2;     // "3분..." | "5분..." | "10분..."

        @NotNull
        private LearningPlace learningPlace;
    }

    @Getter
    @Setter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UpsertResponse {
        private Long profileId;
        private boolean profileCompleted;
    }
}
