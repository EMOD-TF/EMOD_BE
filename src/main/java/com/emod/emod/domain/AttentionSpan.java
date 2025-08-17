package com.emod.emod.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum AttentionSpan {
    SHORT("3분 이내에 자주 산만해져요"),
    MEDIUM("5분 정도는 비교적 집중해요"),
    LONG("10분 이상 대화 흐름을 끝까지 따라요");

    private final String label;

    AttentionSpan(String label) {
        this.label = label;
    }

    @JsonCreator
    public static AttentionSpan fromLabel(String value) {
        for (AttentionSpan a : values()) {
            if (a.label.equals(value)) return a;
        }
        throw new IllegalArgumentException("q2 값이 올바르지 않습니다: " + value);
    }

    @JsonValue
    public String getLabel() {
        return label;
    }
}
