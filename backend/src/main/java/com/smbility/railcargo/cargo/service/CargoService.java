package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.cargo.dto.CargoCorrectionRequest;
import com.smbility.railcargo.cargo.dto.CargoRegisterRequest;
import com.smbility.railcargo.cargo.dto.CargoResponse;
import com.smbility.railcargo.cargo.repository.CargoOrderRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.shipper.service.ShipperService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CargoService {

    private final CargoOrderRepository cargoOrderRepository;
    private final ShipperService shipperService;
    private final CargoAiAnalysisService cargoAiAnalysisService;

    @Transactional
    public CargoResponse register(Long memberId, CargoRegisterRequest request) {
        Shipper shipper = shipperService.getEntityByMemberId(memberId);
        CargoOrder cargoOrder = CargoOrder.register(shipper, request.cargoName(), request.rawInput(),
                request.originStation(), request.destinationStation(), request.desiredDate());
        return CargoResponse.from(cargoOrderRepository.save(cargoOrder));
    }

    @Transactional
    public CargoResponse runAiAnalysis(Long cargoOrderId) {
        CargoOrder cargoOrder = getEntity(cargoOrderId);
        CargoAiAnalysisResult result = cargoAiAnalysisService.analyze(cargoOrder);
        cargoOrder.applyAiAnalysis(result.weightKg(), result.volumeCbm(), result.temperatureCondition(),
                result.hazardous(), result.packagingType(), result.handlingNote());
        return CargoResponse.from(cargoOrder);
    }

    @Transactional
    public CargoResponse correct(Long cargoOrderId, CargoCorrectionRequest request) {
        CargoOrder cargoOrder = getEntity(cargoOrderId);
        cargoOrder.applyShipperCorrection(request.weightKg(), request.volumeCbm(), request.temperatureCondition(),
                request.hazardous(), request.packagingType(), request.handlingNote());
        return CargoResponse.from(cargoOrder);
    }

    public CargoResponse getCargo(Long cargoOrderId) {
        return CargoResponse.from(getEntity(cargoOrderId));
    }

    public List<CargoResponse> getMyCargoOrders(Long memberId) {
        Shipper shipper = shipperService.getEntityByMemberId(memberId);
        return cargoOrderRepository.findAllByShipperId(shipper.getId()).stream()
                .map(CargoResponse::from)
                .toList();
    }

    public CargoOrder getEntity(Long cargoOrderId) {
        return cargoOrderRepository.findById(cargoOrderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "화물 주문을 찾을 수 없습니다."));
    }
}
