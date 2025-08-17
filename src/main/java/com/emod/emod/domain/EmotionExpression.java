package com.emod.emod.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EmotionExpression {
    LOW("거의 표현하지 못해요"),
    MID("짧은 단어나 문장으로 감정을 설명해요"),
    HIGH("상황에 맞춰 감정을 자연스럽게 표현해요");

    private final String label;

    EmotionExpression(String label) {
        this.label = label;
    }

    @JsonCreator
    public static EmotionExpression fromLabel(String value) {
        for (EmotionExpression e : values()) {
            if (e.label.equals(value)) return e;
        }
        throw new IllegalArgumentException("q1 값이 올바르지 않습니다: " + value);
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
