package com.smbility.railcargo.train.controller;

import com.smbility.railcargo.train.dto.TrainResponse;
import com.smbility.railcargo.train.service.TrainService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 화주/운영자 모두 조회 가능한 열차·화차 정보 (읽기 전용). */
@Tag(name = "Train", description = "열차/화차 조회")
@RestController
@RequestMapping("/api/v1/trains")
@RequiredArgsConstructor
public class TrainController {

    private final TrainService trainService;

    @GetMapping
    public List<TrainResponse> getUpcomingTrains() {
        return trainService.getUpcomingTrains();
    }

    @GetMapping("/{trainId}")
    public TrainResponse getTrain(@PathVariable Long trainId) {
        return trainService.getTrain(trainId);
    }
}
