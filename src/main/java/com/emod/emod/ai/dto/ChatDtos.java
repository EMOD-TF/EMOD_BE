package com.emod.emod.ai.dto;

import java.util.List;

public class ChatDtos {
    public record ChatMessage(String role, String content) {
    }

    // OpenAI 요청
    public record ChatCompletionRequest(
            String model,
            List<ChatMessage> messages,
            Double temperature,
            Integer max_tokens
    ) {
    }

    // OpenAI 응답(필요한 필드만)
    public record ChatCompletionResponse(
            List<Choice> choices
    ) {
        public record Choice(Message message) {
        }

        public record Message(String role, String content) {
        }
    }

    // 공통 요청: 누적 대화와 현재 단계
    public record ConversationTurnRequest(
            List<String> conversation,   // "user: ...", "assistant: ..." 등 프론트 누적 메시지 그대로
            Integer currentStep          // 1~4 (장소, 사건, 주제, 감정)
    ) {
    }

    // 1) 진행 API 응답
    public record StepFlowResponse(
            boolean isAnswerValid,
            int nextStep,                 // 1~4, 완료 시 5
            String questionToAsk,         // 다음 질문(검증 실패면 재질문 프롬프트)
            String reason                 // (선택) 검증 근거
    ) {
    }

    // 2) 요약 API 응답
    public record DailySummaryResponse(
            Item place,
            Item event,
            Item topic,
            Item emotion
    ) {
        public record Item(String keyword, String sentence) {
        }
    }
}
