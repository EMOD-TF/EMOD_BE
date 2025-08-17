package com.emod.emod.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "profile")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Profile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "auth_id", unique = true, nullable = false)
    private Auth auth;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(nullable = false)
    private Integer birthYear;

    @Column(nullable = false)
    private Integer birthMonth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    // ✅ 바뀐 부분: q1/q2 → Enum으로 저장, q3 삭제
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EmotionExpression q1;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AttentionSpan q2;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LearningPlace learningPlace;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
