package com.emod.emod.member.service;

import com.emod.emod.domain.Auth;
import com.emod.emod.domain.Profile;
import com.emod.emod.member.dto.ProfileDtos;
import com.emod.emod.member.repository.AuthRepository;
import com.emod.emod.member.repository.ProfileRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;
    private final AuthRepository authRepository;

    public ProfileService(ProfileRepository profileRepository, AuthRepository authRepository) {
        this.profileRepository = profileRepository;
        this.authRepository = authRepository;
    }

    private Long currentAuthId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalStateException("인증이 필요합니다.");
        }
        return (Long) authentication.getPrincipal();
    }

    @Transactional
    public ProfileDtos.UpsertResponse upsert(ProfileDtos.UpsertRequest req, Authentication authentication) {
        Long authId = currentAuthId(authentication);
        Auth auth = authRepository.findById(authId)
                .orElseThrow(() -> new IllegalStateException("Auth가 존재하지 않습니다."));

        Profile profile = profileRepository.findByAuth_Id(authId)
                .map(p -> {
                    p.setName(req.getName());
                    p.setBirthYear(req.getBirthYear());
                    p.setBirthMonth(req.getBirthMonth());
                    p.setGender(req.getGender());
                    p.setQ1(req.getQ1()); // ✅ Enum
                    p.setQ2(req.getQ2()); // ✅ Enum
                    p.setLearningPlace(req.getLearningPlace());
                    return p;
                })
                .orElseGet(() -> Profile.builder()
                        .auth(auth)
                        .name(req.getName())
                        .birthYear(req.getBirthYear())
                        .birthMonth(req.getBirthMonth())
                        .gender(req.getGender())
                        .q1(req.getQ1()) // ✅ Enum
                        .q2(req.getQ2()) // ✅ Enum
                        .learningPlace(req.getLearningPlace())
                        .build());

        Profile saved = profileRepository.save(profile);
        return ProfileDtos.UpsertResponse.builder()
                .profileId(saved.getId())
                .profileCompleted(true)
                .build();
    }
}
