package com.emod.emod.ai.service;

import com.emod.emod.ai.dto.ChatDtos;
import com.emod.emod.ai.dto.ChatDtos.ChatMessage;
import com.emod.emod.ai.dto.Step;
import com.emod.emod.domain.AttentionSpan;
import com.emod.emod.domain.EmotionExpression;
import com.emod.emod.domain.LearningPlace;
import com.emod.emod.domain.Profile;
import com.emod.emod.member.repository.ProfileRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ConversationService {

    private final OpenAiChatService ai;
    private final ProfileRepository profileRepository;

    public ConversationService(OpenAiChatService ai, ProfileRepository profileRepository) {
        this.ai = ai;
        this.profileRepository = profileRepository;
    }

    public ChatDtos.StepFlowResponse proceed(ChatDtos.ConversationTurnRequest req, Authentication authentication) {
        Step step = Step.from(req.currentStep());
        List<String> convo = req.conversation();

        // ① 첫 턴: 맞춤 장소확인 질문(=1번 질문)
        if (convo == null || convo.isEmpty()) {
            String greet = buildPersonalGreeting(authentication);
            return new ChatDtos.StepFlowResponse(false, step.code, greet, "맞춤 인사/첫 질문(장소확인)");
        }

        // ② 이후 단계: q1/q2 특성 반영하여 LLM 프롬프트 구성
        ProfileTraits traits = readTraits(authentication);

        String system = """
                당신은 아동/청소년 일기 작성을 돕는 짧은 대화 코치입니다.
                단계는 1(장소)→2(사건)→3(주제/인물)→4(감정) 순서입니다.
                출력은 반드시 아래 JSON 형식만:
                {
                  "isAnswerValid": true|false,
                  "nextStep": 1..5,
                  "questionToAsk": "다음 질문 한 문장",
                  "reason": "한 문장 근거"
                }
                
                [사용자 특성]
                - 감정 표현: %s
                - 집중 시간: %s
                
                [질문 스타일 가이드]
                - 집중이 짧으면(3분): 질문을 매우 짧고 한 가지 정보만 묻기. 쉬운 단어 사용.
                - 집중이 보통(5분): 간결한 한 문장, 예시 1개 허용.
                - 집중이 김(10분+): 필요한 경우 아주 짧게 추가 맥락을 붙여도 됨(여전히 한 문장).
                - 감정 표현이 낮음: 감정 질문 시 단어 선택지를 예시로 1~2개 제공(기쁨/슬픔 등).
                - 감정 표현이 중간: 간단한 감정 단어로 유도.
                - 감정 표현이 높음: 감정/이유까지 자연스럽게 말해보도록 유도하되 한 문장.
                """.formatted(traits.q1Label, traits.q2Label);

        String user = """
                현재 단계: %d
                이 단계 설명: %s
                누적 대화(lines):
                %s
                마지막 줄이 단계 %d의 질문에 대한 '적절한 답'인지 판정하고, JSON으로만 응답하세요.
                """.formatted(step.code, step.instruction, String.join("\n", convo), step.code);

        JsonNode json = ai.completeAsJson(
                List.of(new ChatMessage("system", system), new ChatMessage("user", user)),
                0.2, 400
        );

        boolean isValid = json.get("isAnswerValid").asBoolean(false);
        int next = json.get("nextStep").asInt(step.code);
        String question = json.get("questionToAsk").asText("");
        String reason = json.has("reason") ? json.get("reason").asText("") : "";

        if (isValid && step == Step.EMOTION) {
            next = 5;
            if (question == null || question.isBlank()) {
                question = "좋아요! 오늘 이야기를 잘 정리했어. 저장할까?";
            }
        }
        if (!isValid) next = step.code;
        if (question == null || question.isBlank()) {
            question = isValid ? firstQuestion(Step.from(Math.min(next, 4)), traits)
                    : retryQuestion(step, traits);
        }

        return new ChatDtos.StepFlowResponse(isValid, next, question, reason);
    }

    // ====== 개인화 정보 ======
    private static class ProfileTraits {
        String name;
        LearningPlace place;
        String q1Label; // 한국어 라벨
        String q2Label;
        EmotionExpression q1;
        AttentionSpan q2;
    }

    private ProfileTraits readTraits(Authentication authentication) {
        ProfileTraits t = new ProfileTraits();
        Long authId = (authentication != null && authentication.getPrincipal() instanceof Long)
                ? (Long) authentication.getPrincipal() : null;

        if (authId == null) return t;

        Optional<Profile> opt = profileRepository.findByAuth_Id(authId);
        if (opt.isEmpty()) return t;

        Profile p = opt.get();
        t.name = p.getName();
        t.place = p.getLearningPlace();
        t.q1 = p.getQ1();
        t.q2 = p.getQ2();
        t.q1Label = (t.q1 != null ? t.q1.getLabel() : "");
        t.q2Label = (t.q2 != null ? t.q2.getLabel() : "");
        return t;
    }

    private String buildPersonalGreeting(Authentication authentication) {
        Long authId = (authentication != null && authentication.getPrincipal() instanceof Long)
                ? (Long) authentication.getPrincipal() : null;

        if (authId == null) return "안녕! 오늘 어디서 있었던 일이야? (예: 유치원/학교/집 등)";

        Optional<Profile> opt = profileRepository.findByAuth_Id(authId);
        if (opt.isEmpty()) return "안녕! 오늘 어디서 있었던 일이야? (예: 유치원/학교/집 등)";

        Profile p = opt.get();
        String name = p.getName();
        String voc = (name == null || name.isBlank()) ? "" : name + suffixForName(name);
        String placeKo = toKoPlace(p.getLearningPlace());

        // ✅ 1번(장소확인질문) 역할을 수행
        if (voc.isBlank()) return "안녕! 오늘 " + placeKo + " 잘 다녀왔어?";
        return "안녕, " + voc + "! 오늘 " + placeKo + " 잘 다녀왔어?";
    }

    private String suffixForName(String name) {
        char last = name.charAt(name.length() - 1);
        if (last >= 0xAC00 && last <= 0xD7A3) {
            int base = last - 0xAC00, jong = base % 28;
            return (jong == 0) ? "야" : "아";
        }
        return "아";
    }

    private String toKoPlace(LearningPlace place) {
        if (place == null) return "유치원";
        return switch (place) {
            case KINDERGARTEN -> "어린이집";
            case SCHOOL -> "학교";
            case HOME -> "집";
            case BUILDING -> "빌딩";
        };
    }

    // 개인화 반영한 기본/재질문
    private String firstQuestion(Step step, ProfileTraits t) {
        String base = switch (step) {
            case PLACE -> "오늘은 어디에서 있었던 일이야? (예: 유치원/학교/집)";
            case EVENT -> "그곳에서 어떤 일이 있었어?";
            case TOPIC -> "그 일의 중심 주제나 중요한 인물은 누가/뭐야?";
            case EMOTION -> "그때 너는 어떤 감정을 느꼈어?";
        };
        return adaptByTraits(base, t);
    }

    private String retryQuestion(Step step, ProfileTraits t) {
        String base = switch (step) {
            case PLACE -> "장소를 한 단어로 말해줄래? (예: 학교, 집)";
            case EVENT -> "오늘 있었던 일을 한 문장으로 말해줄래?";
            case TOPIC -> "이야기의 중심(주제나 인물)을 한 단어로 말해줄래?";
            case EMOTION -> "그때 느낀 감정을 한 단어로 말해줄래? (예: 기쁨/슬픔)";
        };
        return adaptByTraits(base, t);
    }

    // q1/q2에 따라 질문 톤/길이 조정
    private String adaptByTraits(String question, ProfileTraits t) {
        // 집중시간이 짧으면 더 짧은 문장으로
        if (t.q2 == AttentionSpan.SHORT) {
            if (question.length() > 25) question = question.replace("한 문장으로 ", "").replace("어떤 ", "");
        }
        // 감정표현이 낮으면 감정 질문 시 선택지 힌트 추가
        if (t.q1 == EmotionExpression.LOW && question.contains("감정")) {
            question = question + " (예: 기쁨/슬픔)";
        } else if (t.q1 == EmotionExpression.HIGH && question.contains("감정")) {
            question = question + " 한 문장으로 자연스럽게 말해줘.";
        }
        return question;
    }
}
