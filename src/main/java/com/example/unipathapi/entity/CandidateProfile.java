package com.example.unipathapi.entity;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "candidate_profiles")
public class CandidateProfile {

    // 1. Khai báo ID nhưng KHÔNG DÙNG @GeneratedValue nữa
    @Id
    @Column(name = "user_id")
    private Integer  id;

    // 2. Dùng @MapsId để báo cho Spring Boot biết: "Hãy lấy ID của User làm ID cho bảng này"
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "full_name")
    private String fullName;

    @Column(name = "phone_number", length = 10)
    private String phoneNumber;

    @Column(name = "experience_years")
    private Integer experienceYears;

    @Column(name = "university_name")
    private String universityName;

    @Column(name = "major")
    private String major;

    @Column(name = "skills", columnDefinition = "TEXT")
    private String skills;
}
