package com.emod.emod.ai.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfig {

    @Bean
    public WebClient openAiWebClient(OpenAiProperties props) {
        return WebClient.builder()
                .baseUrl(props.getApi().getUrl())
                .defaultHeaders(h -> {
                    h.setBearerAuth(props.getApi().getKey());
                    h.add("Content-Type", "application/json");
                })
                .filter((request, next) -> next.exchange(request)
                        .flatMap(response -> {
                            if (response.statusCode().is4xxClientError() || response.statusCode().is5xxServerError()) {
                                return response.bodyToMono(String.class)
                                        .flatMap(body -> Mono.error(new RuntimeException(
                                                "OpenAI error " + response.statusCode() + ": " + body)));
                            }
                            return Mono.just(response);
                        }))
                .build();
    }
}
