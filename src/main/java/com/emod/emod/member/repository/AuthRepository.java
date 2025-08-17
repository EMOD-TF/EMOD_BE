package com.emod.emod.member.repository;

import com.emod.emod.domain.Auth;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthRepository extends JpaRepository<Auth, Long> {

    Optional<Auth> findByDeviceCode(String deviceCode);

    boolean existsByDeviceCode(String deviceCode);
}
