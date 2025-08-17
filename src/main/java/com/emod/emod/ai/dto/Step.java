package com.emod.emod.ai.dto;

public enum Step {

    PLACE(1, "장소확인질문", "오늘 있었던 일의 장소(유치원/학교/빌딩/집 중 하나 혹은 세부 장소)를 물어봐."),
    EVENT(2, "사건확인질문", "오늘 어떤 사건(상황)이 있었는지 간단히 물어봐."),
    TOPIC(3, "주제확인질문", "오늘 이야기의 중심 주제(혹은 중심 인물/대상)를 물어봐."),
    EMOTION(4, "감정확인질문", "그 일과 관련해 느낀 감정을 물어봐.");

    public final int code;
    public final String label;
    public final String instruction;

    Step(int code, String label, String instruction) {
        this.code = code;
        this.label = label;
        this.instruction = instruction;
    }

    public static Step from(int code) {
        for (Step s : values()) if (s.code == code) return s;
        throw new IllegalArgumentException("유효하지 않은 단계: " + code);
    }
}
