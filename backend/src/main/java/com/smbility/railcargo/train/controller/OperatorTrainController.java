package com.smbility.railcargo.train.controller;

import com.smbility.railcargo.train.dto.TrainRegisterRequest;
import com.smbility.railcargo.train.dto.TrainResponse;
import com.smbility.railcargo.train.dto.WagonRegisterRequest;
import com.smbility.railcargo.train.dto.WagonResponse;
import com.smbility.railcargo.train.service.TrainService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 코레일 운영자용 열차/화차 마스터 데이터 등록 (ROLE_OPERATOR 전용, SecurityConfig에서 제한). */
@Tag(name = "Operator - Train", description = "열차/화차 마스터 데이터 등록")
@RestController
@RequestMapping("/api/v1/operator/trains")
@RequiredArgsConstructor
public class OperatorTrainController {

    private final TrainService trainService;

    @PostMapping
    public ResponseEntity<TrainResponse> registerTrain(@Valid @RequestBody TrainRegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainService.registerTrain(request));
    }

    @PostMapping("/{trainId}/wagons")
    public ResponseEntity<WagonResponse> registerWagon(
            @PathVariable Long trainId,
            @Valid @RequestBody WagonRegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(trainService.registerWagon(trainId, request));
    }
}
