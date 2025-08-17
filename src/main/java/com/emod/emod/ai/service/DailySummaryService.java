package com.emod.emod.ai.service;

import com.emod.emod.ai.dto.ChatDtos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DailySummaryService {

    private final OpenAiChatService ai;

    public DailySummaryService(OpenAiChatService ai) {
        this.ai = ai;
    }

    public ChatDtos.DailySummaryResponse summarize(List<String> conversation) {
        String system = """
                당신은 일기 요약 도우미입니다.
                누적 대화를 바탕으로 아래 JSON만 출력하세요(설명 금지).
                {
                  "place":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "event":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "topic":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "emotion": {"keyword": "기쁨|화남|슬픔 중 하나", "sentence": "한 문장"}
                }
                제약:
                - keyword는 place/event/topic은 가능한 한 단어 1개(짧게).
                - emotion.keyword는 반드시 '기쁨', '화남', '슬픔' 중 하나.
                - sentence는 1문장, 간결하고 사실/느낌을 명확히.
                - 장소가 모호하면 대화에서 가장 가능성 높은 것으로 추정하되, 단어는 일반명사로.
                """;

        String user = "누적 대화(lines):\n" + String.join("\n", conversation);

        JsonNode json = ai.completeAsJson(
                List.of(new ChatDtos.ChatMessage("system", system), new ChatDtos.ChatMessage("user", user)),
                0.2, 300
        );

        JsonNode place = json.get("place");
        JsonNode event = json.get("event");
        JsonNode topic = json.get("topic");
        JsonNode emotion = json.get("emotion");

        return new ChatDtos.DailySummaryResponse(
                new ChatDtos.DailySummaryResponse.Item(
                        safeText(place, "keyword"), safeText(place, "sentence")),
                new ChatDtos.DailySummaryResponse.Item(
                        safeText(event, "keyword"), safeText(event, "sentence")),
                new ChatDtos.DailySummaryResponse.Item(
                        safeText(topic, "keyword"), safeText(topic, "sentence")),
                new ChatDtos.DailySummaryResponse.Item(
                        safeText(emotion, "keyword"), safeText(emotion, "sentence"))
        );
    }

    private String safeText(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText("") : "";
    }
}
