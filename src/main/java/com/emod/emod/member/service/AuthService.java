package com.emod.emod.member.service;

import com.emod.emod.domain.Auth;
import com.emod.emod.member.dto.AuthDtos;
import com.emod.emod.member.repository.AuthRepository;
import com.emod.emod.member.repository.ProfileRepository;
import com.emod.emod.security.JwtProvider;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthRepository authRepository;
    private final ProfileRepository profileRepository;
    private final JwtProvider jwtProvider;

    public AuthService(AuthRepository authRepository,
                       ProfileRepository profileRepository,
                       JwtProvider jwtProvider) {
        this.authRepository = authRepository;
        this.profileRepository = profileRepository;
        this.jwtProvider = jwtProvider;
    }

    @Transactional
    public AuthDtos.SignupResponse signupOrLogin(AuthDtos.SignupRequest req) {
        // deviceCode 존재여부 확인 후 없으면 생성, 있으면 그대로 사용
        Auth auth = authRepository.findByDeviceCode(req.getDeviceCode())
                .orElseGet(() -> authRepository.save(Auth.builder()
                        .deviceCode(req.getDeviceCode())
                        .build()));

        boolean profileCompleted = profileRepository.existsByAuth_Id(auth.getId());
        String token = jwtProvider.generateToken(auth.getId(), auth.getDeviceCode());

        return AuthDtos.SignupResponse.builder()
                .authId(auth.getId())
                .deviceCode(auth.getDeviceCode())
                .profileCompleted(profileCompleted)
                .jwt(token)
                .build();
    }
}
