package com.smbility.railcargo.tracking.support;

import java.util.Map;

/**
 * 역 이름 -> 대략적인 위경도 좌표. 실시간 GPS 위치 시뮬레이션({@code TrackingSimulationService})에서
 * 두 역 사이를 선형보간할 때 기준점으로 쓴다.
 *
 * <p><b>주의:</b> 여기 좌표는 공식 역사 측량 데이터가 아니라 해당 지역의 대략적인 위치를 기준으로
 * 잡은 근사값이다(예: 화물기지/조차장의 정확한 부지 좌표가 아니라 인근 지명 좌표). 실제 서비스화 시
 * 코레일 등에서 제공하는 정확한 역 좌표로 교체해야 한다. 목록에 없는 역은 {@link #DEFAULT}(대한민국 중앙 근사치)를 사용한다.
 */
public final class StationCoordinates {

    /** 알 수 없는 역에 대한 폴백: 대한민국 국토 중앙 부근 근사 좌표. */
    public static final StationCoordinate DEFAULT = new StationCoordinate(36.5, 127.8);

    private static final Map<String, StationCoordinate> COORDINATES = Map.ofEntries(
            Map.entry("서울", new StationCoordinate(37.5547, 126.9707)),
            Map.entry("천안", new StationCoordinate(36.8065, 127.1522)),
            Map.entry("의왕ICD", new StationCoordinate(37.2870, 126.9682)),
            Map.entry("의왕", new StationCoordinate(37.2870, 126.9682)),
            Map.entry("오봉", new StationCoordinate(37.2870, 126.8901)),
            Map.entry("오봉역", new StationCoordinate(37.2870, 126.8901)),
            Map.entry("오봉역(의왕)", new StationCoordinate(37.2870, 126.8901)),
            Map.entry("오송", new StationCoordinate(36.6201, 127.3273)),
            Map.entry("대전", new StationCoordinate(36.3320, 127.4343)),
            Map.entry("동대구", new StationCoordinate(35.8798, 128.6283)),
            Map.entry("동대구역", new StationCoordinate(35.8798, 128.6283)),
            Map.entry("동인천", new StationCoordinate(37.4753, 126.6328)),
            Map.entry("동인천역", new StationCoordinate(37.4753, 126.6328)),
            Map.entry("부산진역", new StationCoordinate(35.1398, 129.0403)),
            Map.entry("부산", new StationCoordinate(35.1152, 129.0424)),
            Map.entry("대전조차장", new StationCoordinate(36.3504, 127.3845))
    );

    public static StationCoordinate of(String stationName) {
        return COORDINATES.getOrDefault(stationName, DEFAULT);
    }

    private StationCoordinates() {
    }
}
