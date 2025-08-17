package com.emod.emod.ai.presentation;

import com.emod.emod.ai.dto.ChatDtos;
import com.emod.emod.ai.service.ConversationService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/convo")
public class ConversationController {

    private final ConversationService service;

    public ConversationController(ConversationService service) {
        this.service = service;
    }

    // JWT 인증 하에 사용 (SecurityConfig에서 /auth/signup만 permitAll 가정)
    @PostMapping("/proceed")
    public ResponseEntity<ChatDtos.StepFlowResponse> proceed(
            @RequestBody @Valid ChatDtos.ConversationTurnRequest req,
            Authentication auth
    ) {
        return ResponseEntity.ok(service.proceed(req, auth));
    }
}
