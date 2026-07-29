package com.example.unipathapi.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "company_members")
public class CompanyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "member_role", length = 20, nullable = false)
    private String memberRole = "RECRUITER"; // 'COMPANY_ADMIN', 'RECRUITER'

    @Column(name = "joined_at", insertable = false, updatable = false)
    private LocalDateTime joinedAt;

    public CompanyMember(Company company, User user, String memberRole) {
        this.company = company;
        this.user = user;
        this.memberRole = memberRole != null ? memberRole : "RECRUITER";
    }
}
