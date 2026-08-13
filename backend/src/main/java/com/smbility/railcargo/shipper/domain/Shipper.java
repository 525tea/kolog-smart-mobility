package com.smbility.railcargo.shipper.domain;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 화주(사업자) 프로필. Member(계정)와 1:1 관계. */
@Getter
@Entity
@Table(name = "shipper")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Shipper extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, unique = true)
    private Member member;

    @Column(name = "business_number", nullable = false, length = 20)
    private String businessNumber;

    @Column(name = "company_name", nullable = false, length = 100)
    private String companyName;

    @Column(name = "manager_name", nullable = false, length = 50)
    private String managerName;

    @Column(nullable = false, length = 30)
    private String phone;

    private Shipper(Member member, String businessNumber, String companyName, String managerName, String phone) {
        this.member = member;
        this.businessNumber = businessNumber;
        this.companyName = companyName;
        this.managerName = managerName;
        this.phone = phone;
    }

    public static Shipper of(Member member, String businessNumber, String companyName, String managerName, String phone) {
        return new Shipper(member, businessNumber, companyName, managerName, phone);
    }
}
