package com.emod.emod.ai.service;

import com.emod.emod.ai.config.OpenAiProperties;
import com.emod.emod.ai.dto.ChatDtos;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class OpenAiChatService {

    private final OpenAiProperties props;
    private final WebClient client;
    private final ObjectMapper om = new ObjectMapper();

    public OpenAiChatService(OpenAiProperties props, WebClient openAiWebClient) {
        this.props = props;
        this.client = openAiWebClient;
    }

    public String complete(List<ChatDtos.ChatMessage> messages, Double temperature, Integer maxTokens) {
        ChatDtos.ChatCompletionRequest req = new ChatDtos.ChatCompletionRequest(
                props.getModel(),
                messages,
                temperature != null ? temperature : props.getOptions().getTemperature(),
                maxTokens != null ? maxTokens : props.getOptions().getMaxTokens()
        );

        ChatDtos.ChatCompletionResponse resp = client.post()
                .bodyValue(req)
                .retrieve()
                .bodyToMono(ChatDtos.ChatCompletionResponse.class)
                .block();

        if (resp == null || resp.choices() == null || resp.choices().isEmpty()) {
            throw new IllegalStateException("OpenAI 응답이 비어 있습니다.");
        }
        return resp.choices().get(0).message().content();
    }

    public JsonNode completeAsJson(List<ChatDtos.ChatMessage> messages, Double temperature, Integer maxTokens) {
        String content = complete(messages, temperature, maxTokens);
        try {
            return om.readTree(content);
        } catch (Exception e) {
            // 모델이 JSON이 아니게 답하면, 마지막 안전장치로 JSON만 추출 시도
            throw new IllegalStateException("LLM 응답 JSON 파싱 실패: " + content);
        }
    }
}
