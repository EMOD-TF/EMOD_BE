package com.emod.emod.ai.service;

import com.emod.emod.ai.dto.ChatDtos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class DailySummaryService {

    private final OpenAiChatService ai;

    public DailySummaryService(OpenAiChatService ai) {
        this.ai = ai;
    }

    public ChatDtos.DailySummaryResponse summarize(List<String> conversation) {
        String system = """
                당신은 '아이에게 말해주는 일기 요약 도우미'입니다.
                누적 대화를 바탕으로 아래 JSON만 출력하세요(설명 금지).
                
                {
                  "place":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "event":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "topic":   {"keyword": "단어 1개", "sentence": "한 문장"},
                  "emotion": {"keyword": "기쁨|화남|슬픔 중 하나", "sentence": "한 문장"}
                }
                
                제약:
                - place/event/topic의 keyword는 가급적 1단어(최소화).
                - emotion.keyword는 반드시 '기쁨'|'화남'|'슬픔' 중 하나.
                - 모든 sentence는 아이에게 말해주는 톤으로, **완결된 평서+감탄형 한 문장**이어야 한다.
                  (예: "~했구나!", "~였구나!", "~했겠구나!", "~이었겠구나!")
                - 문장 파편(예: "친구들과 함께", "상을 받음") 금지. **서술어 포함 필수**.
                - 장소가 모호하면 가장 가능성 높은 일반명사로 추정하되 keyword는 일반명사 1개로.
                - 출력은 **JSON만**, 여분의 텍스트/주석 금지.
                
                [좋은 예]
                "친구들과 사이좋게 놀았구나!"
                "블록 쌓기 대회에서 상을 받았구나!"
                "상을 받아 뿌듯함을 느꼈겠구나!"
                "정말 기뻤겠구나!"
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

        String placeKeyword = safeText(place, "keyword");
        String eventKeyword = safeText(event, "keyword");
        String topicKeyword = safeText(topic, "keyword");
        String emotionKeyword = safeText(emotion, "keyword");

        String placeSentence = toChildTone(safeText(place, "sentence"));
        String eventSentence = toChildTone(safeText(event, "sentence"));
        String topicSentence = toChildTone(safeText(topic, "sentence"));
        String emotionSentence = toChildToneEmotion(safeText(emotion, "sentence"), emotionKeyword);

        return new ChatDtos.DailySummaryResponse(
                new ChatDtos.DailySummaryResponse.Item(placeKeyword, placeSentence),
                new ChatDtos.DailySummaryResponse.Item(eventKeyword, eventSentence),
                new ChatDtos.DailySummaryResponse.Item(topicKeyword, topicSentence),
                new ChatDtos.DailySummaryResponse.Item(emotionKeyword, emotionSentence)
        );
    }

    private String safeText(JsonNode node, String field) {
        return node != null && node.has(field) ? node.get(field).asText("") : "";
    }

    /**
     * 일반 문장을 아이에게 말해주는 말투로 정돈: "~했구나!", "~였구나!", "~했겠구나!" 등
     * - 명사형 파편("~받음", "~느낌", "~중")을 자연스러운 동사/형용사형으로 변환
     * - 종결을 감탄형으로 강제
     */
    private String toChildTone(String s) {
        if (s == null) return "";
        s = s.trim();

        if (s.isEmpty()) return s;

        // 1) 흔한 명사형·파편 보정
        s = s
                // 받음/경험 계열 → 받았구나
                .replaceAll("(받음|수상|획득)$", "받았구나!")
                .replaceAll("받았습니다\\.?$", "받았구나!") // 혹시 존댓말 섞여 오면
                // 느낌/느꼈음 → 느꼈겠구나
                .replaceAll("(느낌|느꼈음)$", "느꼈겠구나!")
                // 진행 중 → 하고 있었구나
                .replaceAll("(중|중임|중이었음)$", "하고 있었구나!")
                // 함께 → 함께 했구나
                .replaceAll("함께$", "함께 했구나!")
                // 명사형 함/됨 → 했구나/되었구나
                .replaceAll("함$", "했구나!")
                .replaceAll("됨$", "되었구나!");

        // 2) 문장 끝이 마침표/비감탄이면 감탄형으로 변환
        // 과거 서술형을 감탄형으로 스왑
        s = s.replaceAll("했다\\.?$", "했구나!");
        s = s.replaceAll("하였다\\.?$", "했구나!");
        s = s.replaceAll("였다\\.?$", "였구나!");
        s = s.replaceAll("이었다\\.?$", "이었구나!");
        s = s.replaceAll("였다\\!$", "였구나!");
        s = s.replaceAll("이었다\\!$", "이었구나!");
        s = s.replaceAll("되었다\\.?$", "되었구나!");
        s = s.replaceAll("느꼈다\\.?$", "느꼈구나!");
        s = s.replaceAll("좋았다\\.?$", "좋았구나!");
        s = s.replaceAll("싫었다\\.?$", "싫었구나!");

        // 3) 단순 과거 종결(다/다.) → 안전한 감탄형으로 전환
        if (Pattern.compile("(다\\.?|요\\.?|\\.)$").matcher(s).find() && !s.endsWith("!")) {
            // 문장 끝의 "다." 또는 "다"를 "구나!" 계열로 치환 시도
            s = s.replaceAll("다\\.$", "구나!");
            s = s.replaceAll("다$", "구나!");
            s = s.replaceAll("요\\.$", "구나!");
            s = s.replaceAll("\\.$", "!");
        }

        // 4) 여전히 동사/형용사 종결이 없거나 너무 파편이라면 보수적으로 감탄형 부착
        if (!s.endsWith("!")) {
            s = s + "!";
        }

        // 5) 과도한 중복 부호 정리
        s = s.replaceAll("!!+$", "!");
        return s;
    }

    /**
     * 감정 문장은 감정 단어에 맞춰 더 자연스럽게 보정
     */
    private String toChildToneEmotion(String s, String emotionKeyword) {
        s = toChildTone(s);

        // 감정 키워드 힌트 기반 미세 조정
        if (emotionKeyword != null) {
            emotionKeyword = emotionKeyword.trim();
            if (emotionKeyword.equals("기쁨")) {
                // '기뻤'을 유도
                s = s.replaceAll("기쁘(었|웠)?구나!", "기뻤구나!");
                if (!s.matches(".*기뻤.*")) {
                    s = ensureEndsWithFeeling(s, "기뻤겠구나!");
                }
            } else if (emotionKeyword.equals("슬픔")) {
                s = s.replaceAll("슬프(었|웠)?구나!", "슬펐구나!");
                if (!s.matches(".*슬펐.*")) {
                    s = ensureEndsWithFeeling(s, "슬펐겠구나!");
                }
            } else if (emotionKeyword.equals("화남")) {
                if (!s.matches(".*화가 났.*")) {
                    s = ensureEndsWithFeeling(s, "화가 났겠구나!");
                }
            }
        }
        return s;
    }

    private String ensureEndsWithFeeling(String s, String feelingEnding) {
        // 문장 끝을 부드럽게 감정으로 마무리
        if (s.endsWith("!")) s = s.substring(0, s.length() - 1);
        // 문장이 너무 짧거나 동작 서술이 없으면 감정만 붙여도 어색하지 않음
        if (!s.endsWith("구나")) {
            return s + ", " + feelingEnding;
        } else {
            return s + " " + feelingEnding;
        }
    }
}
