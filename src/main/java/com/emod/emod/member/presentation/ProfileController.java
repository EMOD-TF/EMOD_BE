package com.emod.emod.member.presentation;

import com.emod.emod.member.dto.ProfileDtos;
import com.emod.emod.member.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    // 2) 추가 정보 입력/수정(업서트). 인증 필요(Authorization: Bearer <JWT>)
    @PostMapping
    public ResponseEntity<ProfileDtos.UpsertResponse> upsert(
            @RequestBody @Valid ProfileDtos.UpsertRequest req,
            Authentication authentication
    ) {
        return ResponseEntity.ok(profileService.upsert(req, authentication));
    }

}
