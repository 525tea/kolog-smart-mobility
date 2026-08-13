package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.domain.CargoOrder;
import com.smbility.railcargo.cargo.dto.CargoAiAnalysisResult;
import com.smbility.railcargo.cargo.dto.CargoAnalysisResponse;
import com.smbility.railcargo.cargo.dto.CargoCorrectionRequest;
import com.smbility.railcargo.cargo.dto.CargoMsdsFile;
import com.smbility.railcargo.cargo.dto.CargoRegisterRequest;
import com.smbility.railcargo.cargo.dto.CargoResponse;
import com.smbility.railcargo.cargo.dto.StationMappingResponse;
import com.smbility.railcargo.cargo.domain.ServiceMode;
import com.smbility.railcargo.cargo.repository.CargoOrderRepository;
import com.smbility.railcargo.common.exception.BusinessException;
import com.smbility.railcargo.common.exception.ErrorCode;
import com.smbility.railcargo.notification.domain.NotificationType;
import com.smbility.railcargo.notification.service.NotificationService;
import com.smbility.railcargo.shipper.domain.Shipper;
import com.smbility.railcargo.shipper.service.ShipperService;
import java.util.List;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CargoService {

    private final CargoOrderRepository cargoOrderRepository;
    private final ShipperService shipperService;
    private final CargoAiAnalysisService cargoAiAnalysisService;
    private final NotificationService notificationService;
    private final CargoStationMappingService stationMappingService;

    @Transactional
    public CargoResponse register(Long memberId, CargoRegisterRequest request) {
        Shipper shipper = shipperService.getEntityByMemberId(memberId);
        StationMappingResponse originMapping = stationMappingService.map(request.originStation());
        StationMappingResponse destinationMapping = stationMappingService.map(request.destinationStation());
        boolean outOfCoverage = originMapping.outOfCoverage() || destinationMapping.outOfCoverage();
        if (outOfCoverage && request.serviceMode() != ServiceMode.INDIVIDUAL) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    originMapping.outOfCoverage() ? originMapping.userMessage() : destinationMapping.userMessage());
        }
        String originStation = originMapping.outOfCoverage() ? request.originStation().trim() : originMapping.railStation();
        String destinationStation = destinationMapping.outOfCoverage() ? request.destinationStation().trim() : destinationMapping.railStation();
        if (request.serviceMode() == ServiceMode.CO_LOAD && originStation.equals(destinationStation)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출발지와 도착지가 같은 철도 거점으로 매핑되었습니다.");
        }
        CargoOrder cargoOrder = CargoOrder.register(shipper, request.cargoName(), request.rawInput(),
                originStation, destinationStation, request.desiredDate(),
                request.declaredValueKrw(), request.serviceMode());
        CargoOrder saved = cargoOrderRepository.save(cargoOrder);

        notificationService.notify(memberId, NotificationType.INFO, "화물 등록 완료",
                saved.getCargoName() + " 화물이 등록되었습니다. AI 분석을 진행해주세요.");
        return CargoResponse.from(saved);
    }

    @Transactional
    public CargoAnalysisResponse runAiAnalysis(Long memberId, Long cargoOrderId) {
        CargoOrder cargoOrder = getOwnedEntity(memberId, cargoOrderId);
        CargoAiAnalysisResult result = cargoAiAnalysisService.analyze(cargoOrder);
        cargoOrder.applyAiAnalysis(result.weightKg(), result.volumeCbm(), result.temperatureCondition(),
                result.hazardous(), result.hazardGrade(), result.hazardClassCode(), result.hazardClassName(),
                result.rejected(), result.requiresMsds(), result.surchargeRate(), result.fixedPowerFeeKrw(),
                result.detectedTemperatureC(), result.packagingType(), result.handlingNote());

        notificationService.notify(cargoOrder.getShipper().getMember().getId(), NotificationType.ANALYSIS,
                "AI 분석 완료", cargoOrder.getCargoName() + " 화물의 운송조건 분석이 완료됐어요. 결과를 확인해주세요.");
        return CargoAnalysisResponse.of(CargoResponse.from(cargoOrder), result);
    }

    @Transactional
    public CargoResponse correct(Long memberId, Long cargoOrderId, CargoCorrectionRequest request) {
        CargoOrder cargoOrder = getOwnedEntity(memberId, cargoOrderId);
        if (cargoOrder.isTransportRejected() && Boolean.FALSE.equals(request.hazardous())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "1급 위험물 판정은 화주 수정으로 해제할 수 없습니다. 관리자 검토가 필요합니다.");
        }
        cargoOrder.applyShipperCorrection(request.weightKg(), request.volumeCbm(), request.temperatureCondition(),
                request.hazardous(), request.hazardGrade(), request.packagingType(), request.handlingNote());
        return CargoResponse.from(cargoOrder);
    }

    @Transactional
    public CargoResponse attachMsds(Long memberId, Long cargoOrderId, MultipartFile file) {
        CargoOrder cargoOrder = getOwnedEntity(memberId, cargoOrderId);
        if (!cargoOrder.isRequiresMsds()) {
            throw new BusinessException(ErrorCode.INVALID_STATE_TRANSITION, "MSDS 제출 대상 화물이 아닙니다.");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "MSDS 파일을 선택해주세요.");
        }
        if (file.getSize() > 20L * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PAYLOAD_TOO_LARGE, "MSDS 파일은 20MB 이하만 제출할 수 있습니다.");
        }
        String originalName = file.getOriginalFilename() == null ? "msds.pdf" : file.getOriginalFilename();
        String normalizedName = originalName.replace('\\', '/');
        String safeName = normalizedName.substring(normalizedName.lastIndexOf('/') + 1);
        if (safeName.isBlank()) safeName = "msds.pdf";
        if (!safeName.toLowerCase().matches(".*\\.(pdf|png|jpg|jpeg)$")) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "MSDS는 PDF·PNG·JPG 파일만 제출할 수 있습니다.");
        }
        try {
            cargoOrder.attachMsds(safeName, file.getContentType(), file.getBytes());
            return CargoResponse.from(cargoOrder);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.DOCUMENT_PROCESSING_FAILED, "MSDS 파일을 저장하지 못했습니다.");
        }
    }

    public CargoMsdsFile getMsds(Long memberId, Long cargoOrderId) {
        CargoOrder cargoOrder = getOwnedEntity(memberId, cargoOrderId);
        if (!cargoOrder.isMsdsAttached()) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "제출된 MSDS 파일이 없습니다.");
        }
        String contentType = cargoOrder.getMsdsContentType();
        if (contentType == null || contentType.isBlank()) contentType = "application/octet-stream";
        return new CargoMsdsFile(cargoOrder.getMsdsFileName(), contentType, cargoOrder.getMsdsData());
    }

    public StationMappingResponse previewStationMapping(String location) {
        return stationMappingService.map(location);
    }

    public CargoResponse getCargo(Long memberId, Long cargoOrderId) {
        return CargoResponse.from(getOwnedEntity(memberId, cargoOrderId));
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

    public CargoOrder getOwnedEntity(Long memberId, Long cargoOrderId) {
        CargoOrder cargoOrder = getEntity(cargoOrderId);
        if (!cargoOrder.getShipper().getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED, "본인의 화물만 조회하거나 변경할 수 있습니다.");
        }
        return cargoOrder;
    }
}
