package com.smbility.railcargo.shipper.service;

import com.smbility.railcargo.auth.domain.Member;
import com.smbility.railcargo.auth.domain.MemberRole;
import com.smbility.railcargo.auth.repository.MemberRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.shipper.dto.ShipperRegisterRequest;
import com.smbility.railcargo.shipper.dto.ShipperResponse;
import com.smbility.railcargo.shipper.repository.ShipperRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShipperService {

    private final ShipperRepository shipperRepository;
    private final MemberRepository memberRepository;

    @Transactional
    public ShipperResponse register(Long memberId, ShipperRegisterRequest request) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "회원을 찾을 수 없습니다."));

        if (!member.isRole(MemberRole.SHIPPER)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "화주 계정만 사업자 등록을 할 수 있습니다.");
        }

        shipperRepository.findByMemberId(memberId).ifPresent(existing -> {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "이미 사업자 등록이 완료된 계정입니다.");
        });

        Shipper shipper = Shipper.of(member, request.businessNumber(), request.companyName(),
                request.managerName(), request.phone());
        return ShipperResponse.from(shipperRepository.save(shipper));
    }

    public ShipperResponse getMyProfile(Long memberId) {
        Shipper shipper = shipperRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사업자 등록 정보가 없습니다."));
        return ShipperResponse.from(shipper);
    }

    /** 다른 도메인 서비스(cargo 등)에서 memberId로 Shipper 엔티티가 필요할 때 사용한다. */
    public Shipper getEntityByMemberId(Long memberId) {
        return shipperRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "사업자 등록 정보가 없습니다."));
    }
}
