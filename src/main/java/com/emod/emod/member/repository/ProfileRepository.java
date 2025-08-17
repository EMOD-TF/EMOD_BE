package com.emod.emod.member.repository;

import com.emod.emod.domain.Profile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByAuth_Id(Long authId);

    Optional<Profile> findByAuth_Id(Long authId);
}
