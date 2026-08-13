package com.smbility.railcargo.train.domain;

import com.smbility.railcargo.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 코레일 운행 열차 마스터 데이터. */
@Getter
@Entity
@Table(name = "train")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Train extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "train_number", nullable = false, length = 20)
    private String trainNumber;

    @Column(name = "origin_station", nullable = false, length = 50)
    private String originStation;

    @Column(name = "destination_station", nullable = false, length = 50)
    private String destinationStation;

    @Column(name = "departure_at", nullable = false)
    private LocalDateTime departureAt;

    @Column(name = "arrival_at", nullable = false)
    private LocalDateTime arrivalAt;

    @Column(name = "reservation_deadline", nullable = false)
    private LocalDateTime reservationDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainStatus status;

    private Train(String trainNumber, String originStation, String destinationStation,
                   LocalDateTime departureAt, LocalDateTime arrivalAt, LocalDateTime reservationDeadline) {
        this.trainNumber = trainNumber;
        this.originStation = originStation;
        this.destinationStation = destinationStation;
        this.departureAt = departureAt;
        this.arrivalAt = arrivalAt;
        this.reservationDeadline = reservationDeadline;
        this.status = TrainStatus.SCHEDULED;
    }

    public static Train of(String trainNumber, String originStation, String destinationStation,
                            LocalDateTime departureAt, LocalDateTime arrivalAt, LocalDateTime reservationDeadline) {
        return new Train(trainNumber, originStation, destinationStation, departureAt, arrivalAt, reservationDeadline);
    }

    public boolean isReservable() {
        return status == TrainStatus.SCHEDULED && LocalDateTime.now().isBefore(reservationDeadline);
    }
}
