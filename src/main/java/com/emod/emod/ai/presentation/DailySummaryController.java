package com.emod.emod.ai.presentation;

import com.emod.emod.ai.dto.ChatDtos;
import com.emod.emod.ai.service.DailySummaryService;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/summary")
public class DailySummaryController {

    private final DailySummaryService service;

    public DailySummaryController(DailySummaryService service) {
        this.service = service;
    }

    public record SummaryRequest(@NotEmpty List<String> conversation) {
    }

    @PostMapping("/today")
    public ResponseEntity<ChatDtos.DailySummaryResponse> summarize(
            @RequestBody SummaryRequest req,
            Authentication auth
    ) {
        return ResponseEntity.ok(service.summarize(req.conversation()));
    }
}
