package com.emod.emod.ai.service;

import com.emod.emod.ai.dto.ChatDtos;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
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
                누적 대화를 바탕으로 아래 **JSON만** 출력하세요(설명 금지).
                
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
                - **각 sentence는 40~70자(권장 50자 내외)**로, 원인/상황·느낌을 함께 담는다.
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
                0.2, 600
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

        // 40~70자 범위로 확장(권장 50자 전후). 부족하면 템플릿으로 강제 확장.
        placeSentence = expandForLength(placeSentence, 40, 70, "place", emotionKeyword, placeKeyword);
        eventSentence = expandForLength(eventSentence, 40, 70, "event", emotionKeyword, eventKeyword);
        topicSentence = expandForLength(topicSentence, 40, 70, "topic", emotionKeyword, topicKeyword);
        emotionSentence = expandForLength(emotionSentence, 40, 70, "emotion", emotionKeyword, null);

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

    /**
     * 길이 확장기: sentence가 minLength 미만이면 자연스러운 꼬릿말을 덧붙여 40~70자(권장 50자 내외)로 확장.
     * 부족하면 강제 템플릿(hardTemplateExpand)로 50자 안팎 보장.
     *
     * @param type           place/event/topic/emotion
     * @param emotionKeyword 감정 키워드(감정 전용 tail 선택용)
     * @param primaryKeyword 유형별 핵심 키워드(예: place=장소명)
     */
    private String expandForLength(String s, int minLength, int maxLength, String type, String emotionKeyword, String primaryKeyword) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return s;

        if (s.length() >= minLength && s.length() <= maxLength) return ensureExclamationEnding(s);

        // 꼬릿말 후보들
        String[] commonTails = new String[]{
                " 그래서 더 기억에 남았겠구나!",
                " 덕분에 하루가 특별했겠구나!",
                " 그런 순간이 참 소중했겠구나!",
                " 네 마음이 참 따뜻했겠구나!",
                " 너에게 큰 의미가 있었겠구나!"
        };
        String[] placeTails = new String[]{
                " 그곳 분위기가 너를 기쁘게 했겠구나!",
                " 낯설지만 설레는 느낌이 들었겠구나!",
                " 편안해서 오래 머물고 싶었겠구나!"
        };
        String[] eventTails = new String[]{
                " 준비한 만큼 스스로도 대견했겠구나!",
                " 작은 실수도 배움이 되었겠구나!",
                " 다음엔 더 잘해보고 싶었겠구나!"
        };
        String[] topicTails = new String[]{
                " 궁금했던 점이 풀려서 속이 시원했겠구나!",
                " 생각이 자라나는 느낌이 들었겠구나!",
                " 이야기할 거리도 많아졌겠구나!"
        };
        String[] joyTails = new String[]{
                " 웃음이 절로 났겠구나!",
                " 마음이 반짝반짝 빛났겠구나!",
                " 온종일 발걸음이 가벼웠겠구나!"
        };
        String[] sadTails = new String[]{
                " 마음이 조금 무거웠겠구나!",
                " 그래도 잘 버텨줘서 고맙구나!",
                " 시간이 지나며 나아졌겠구나!"
        };
        String[] angryTails = new String[]{
                " 속이 답답하고 불편했겠구나!",
                " 천천히 진정하려고 애썼겠구나!",
                " 다음에는 더 지혜롭게 풀겠구나!"
        };

        // 느낌표 보장 후 시작
        s = ensureExclamationEnding(s);

        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < minLength) {
            String tail = pickTail(type, emotionKeyword, commonTails, placeTails, eventTails, topicTails, joyTails, sadTails, angryTails);
            appendTail(sb, tail, maxLength);
            if (tail == null) break;
        }

        String out = sb.toString();

        // 여전히 짧으면 강제 템플릿 확장(50자 안팎 보장)
        if (out.length() < minLength) {
            out = hardTemplateExpand(out, type, primaryKeyword, emotionKeyword, (minLength + maxLength) / 2);
        }

        // 너무 길면 부드럽게 자르기
        if (out.length() > maxLength) {
            out = softTrim(out, maxLength);
        }
        return ensureExclamationEnding(out);
    }

    // 유형/감정에 맞춘 tail 선택
    private String pickTail(
            String type, String emotionKeyword,
            String[] commonTails, String[] placeTails, String[] eventTails, String[] topicTails,
            String[] joyTails, String[] sadTails, String[] angryTails
    ) {
        ThreadLocalRandom r = ThreadLocalRandom.current();

        if ("emotion".equals(type)) {
            if ("기쁨".equals(emotionKeyword)) return joyTails[r.nextInt(joyTails.length)];
            if ("슬픔".equals(emotionKeyword)) return sadTails[r.nextInt(sadTails.length)];
            if ("화남".equals(emotionKeyword)) return angryTails[r.nextInt(angryTails.length)];
            return commonTails[r.nextInt(commonTails.length)];
        }

        if ("place".equals(type)) {
            if (r.nextDouble() < 0.6) return placeTails[r.nextInt(placeTails.length)];
        } else if ("event".equals(type)) {
            if (r.nextDouble() < 0.6) return eventTails[r.nextInt(eventTails.length)];
        } else if ("topic".equals(type)) {
            if (r.nextDouble() < 0.6) return topicTails[r.nextInt(topicTails.length)];
        }

        return commonTails[r.nextInt(commonTails.length)];
    }

    private void appendTail(StringBuilder sb, String tail, int maxLength) {
        if (tail == null) return;
        // 기존 끝의 느낌표 제거(중복 방지)
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == '!') {
            sb.setLength(sb.length() - 1);
        }
        String add = " " + tail.trim();
        // 약간 초과는 softTrim으로 정리
        if (sb.length() + add.length() <= maxLength + 10) {
            sb.append(add);
        }
    }

    private String softTrim(String s, int maxLen) {
        if (s.length() <= maxLen) return s;
        // "겠구나!" 또는 "구나!"를 기준으로 자연스럽게 자르기
        int idx = s.lastIndexOf("겠구나!");
        if (idx == -1) idx = s.lastIndexOf("구나!");
        if (idx != -1 && idx + 4 <= s.length()) {
            String cand = s.substring(0, idx + 4);
            if (cand.length() >= maxLen - 5) return ensureExclamationEnding(cand);
        }
        // 그래도 길면 하드 트림
        String cut = s.substring(0, Math.min(maxLen, s.length())).replaceAll("[!]*$", "");
        return ensureExclamationEnding(cut);
    }

    private String ensureExclamationEnding(String s) {
        String out = s == null ? "" : s.trim();
        if (!out.endsWith("!")) out = out + "!";
        return out.replaceAll("!!+$", "!");
    }

    // ★ 유형별 강제 템플릿 확장: 40자 미만 등 매우 짧을 때 50자 안팎으로 확장
    private String hardTemplateExpand(String s, String type, String keyword, String emotionKeyword, int targetLen) {
        if (s == null) s = "";
        s = s.trim().replaceAll("!!+$", "!");
        final String k = (keyword == null || keyword.isBlank()) ? "" : keyword.trim();

        String base = s;
        // 자연스러운 연결을 위해 느낌표 잠시 제거
        if (base.endsWith("!")) base = base.substring(0, base.length() - 1);

        String tail;
        switch (type) {
            case "place":
                tail = " 오늘 " + (k.isEmpty() ? "그곳" : k) + "에서 친구들과 함께 놀고 배우며 "
                        + "즐거운 순간을 차곡차곡 쌓았겠구나";
                break;
            case "event":
                tail = " 준비한 만큼 마음이 뿌듯했고 다음에도 멋지게 해보고 싶었겠구나";
                break;
            case "topic":
                tail = " 궁금했던 점이 한 가지씩 풀리면서 네 생각이 더 자라났겠구나";
                break;
            case "emotion":
                if ("기쁨".equals(emotionKeyword)) {
                    tail = " 마음이 반짝반짝 빛나서 온종일 미소가 떠나지 않았겠구나";
                } else if ("슬픔".equals(emotionKeyword)) {
                    tail = " 마음이 조금 무거웠지만 차분히 잘 이겨내려 했겠구나";
                } else { // 화남
                    tail = " 속이 답답했지만 깊게 숨 쉬며 천천히 가라앉히려 했겠구나";
                }
                break;
            default:
                tail = " 그래서 오늘 하루가 너에게 더욱 특별한 의미로 남았겠구나";
        }

        String out = (base + ", " + tail + "!").replaceAll("!!+$", "!");
        // 목표 길이 근처가 되도록 보정(너무 짧으면 살짝 늘리기)
        if (out.length() < targetLen - 8) {
            // 길이만 맞추는 하드 보정: 안전하게 느낌표 보존
            int pad = Math.min(Math.max(targetLen - out.length(), 0), 8);
            if (pad > 0)
                out = out.replaceAll("!$", "") + " 정말 그랬겠구나!".substring(0, Math.min(pad + 1, " 정말 그랬겠구나!".length())) + "!";
        }
        return ensureExclamationEnding(out);
    }
}
