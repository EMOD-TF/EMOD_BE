package com.emod.emod.member.presentation;

import com.emod.emod.member.dto.AuthDtos;
import com.emod.emod.member.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    // 1) 시작하기 클릭 시: 디바이스 코드로 가입/로그인 + JWT 발급 + profileCompleted 여부 반환
    @PostMapping("/signup")
    public ResponseEntity<AuthDtos.SignupResponse> signup(@RequestBody @Valid AuthDtos.SignupRequest req) {
        return ResponseEntity.ok(authService.signupOrLogin(req));
    }
}
