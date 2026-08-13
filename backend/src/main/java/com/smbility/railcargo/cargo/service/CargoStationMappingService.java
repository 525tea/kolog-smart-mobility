package com.smbility.railcargo.cargo.service;

import com.smbility.railcargo.cargo.dto.StationMappingResponse;
import java.util.List;
import org.springframework.stereotype.Service;

/** Notion 기획안의 19개 권역 규칙으로 주소·지역명을 코레일 화물역에 매핑한다. */
@Service
public class CargoStationMappingService {

    private static final List<StationRule> STATIONS = List.of(
            rule("ST_OBONG", "오봉역(의왕)", "서울", "강남", "서초", "송파", "인천", "부천", "광명", "안양", "과천", "군포", "성남", "수원", "용인", "하남", "구리", "남양주"),
            rule("ST_SEOHWASEONG", "서화성역", "화성", "안산", "시흥", "동탄"),
            rule("ST_DUJEONG", "두정역(천안)", "천안", "아산", "안성", "평택"),
            rule("ST_SAPGYO", "삽교역", "예산", "홍성", "당진", "서산", "태안", "보령"),
            rule("ST_BUGANG", "부강화물역(세종)", "세종", "청주", "공주", "진천"),
            rule("ST_SINTANJIN", "신탄진역(대전)", "대전", "유성", "옥천", "금산", "계룡", "논산"),
            rule("ST_BUSANJIN", "부산진역", "부산", "해운대", "서면", "양산", "김해", "밀양"),
            rule("ST_BUKCHUL", "북철송장(부산신항)", "강서구", "명지", "창원", "마산", "진해", "거제", "통영"),
            rule("ST_YAKMOK", "약목역(칠곡)", "대구", "수성구", "달서구", "칠곡", "구미", "김천", "상주", "성주"),
            rule("ST_ULSAN", "울산신항역", "울산", "남구", "중구", "울주"),
            rule("ST_YEONGILMAN", "영일만항역(포항)", "포항", "경주", "영천", "영덕", "울진"),
            rule("ST_DONGIKSAN", "동익산역", "익산", "김제", "정읍", "부안", "서천"),
            rule("ST_BUKJEONJU", "북전주역", "전주", "완주", "진안", "무주", "남원"),
            rule("ST_GUNSAN", "군산항역", "군산"),
            rule("ST_GWANGYANG", "신광양항역", "광주", "광양", "여수", "순천", "목포", "나주"),
            rule("ST_YEONGJU", "영주역", "영주", "안동", "예천", "문경", "단양"),
            rule("ST_SEOKPO", "석포역", "봉화", "태백"),
            rule("ST_DONGHAE", "동해역", "동해", "삼척", "정선"),
            rule("ST_ANIN", "안인역(강릉)", "강릉", "속초", "양양", "평창")
    );

    private static final List<StationRule> DIRECT = List.of(
            rule("ST_OBONG", "오봉역(의왕)", "경기", "경기도"),
            rule("ST_OBONG", "의왕ICD", "의왕ICD"), rule("ST_OBONG", "오봉역(의왕)", "오봉역"),
            rule("ST_BUKCHUL", "부산신항역", "부산신항역"), rule("ST_BUSANJIN", "부산진역", "부산진역"),
            rule("ST_ULSAN", "울산항역", "울산항역"), rule("ST_YEONGILMAN", "포항역", "포항역"),
            rule("ST_DUJEONG", "천안역", "천안역"), rule("ST_YAKMOK", "동대구역", "동대구역"),
            rule("ST_SINTANJIN", "대전조차장역", "대전조차장역"), rule("ST_GWANGYANG", "광주송정역", "광주송정역"),
            rule("ST_OBONG", "동인천역", "동인천역"));

    public StationMappingResponse map(String inputLocation) {
        String normalized = inputLocation == null ? "" : inputLocation.trim();
        if (normalized.isBlank()) return out(inputLocation, "출발지와 도착지를 입력해주세요.");
        String compact = normalized.replaceAll("\\s+", "");
        for (StationRule station : DIRECT) {
            if (station.keywords().stream().anyMatch(compact::equals)) return matched(inputLocation, station, false);
        }
        StationRule bestMatch = null;
        int bestKeywordLength = -1;
        for (StationRule station : STATIONS) {
            String stationName = station.stationName().replaceAll("\\s+", "");
            if (compact.contains(stationName) && stationName.length() > bestKeywordLength) {
                bestMatch = station;
                bestKeywordLength = stationName.length();
            }
            for (String keyword : station.keywords()) {
                String normalizedKeyword = keyword.replaceAll("\\s+", "");
                if (compact.contains(normalizedKeyword) && normalizedKeyword.length() > bestKeywordLength) {
                    bestMatch = station;
                    bestKeywordLength = normalizedKeyword.length();
                }
            }
        }
        if (bestMatch != null) return matched(inputLocation, bestMatch, true);
        return out(inputLocation, "해당 지역은 철도 화물역과 거리가 멉니다. 100% 트럭 직배송 견적을 확인해주세요.");
    }

    private StationMappingResponse matched(String input, StationRule station, boolean mapped) {
        return new StationMappingResponse(input, station.stationName(), mapped, station.stationId(), false,
                mapped ? "가장 가까운 화물 전용 기지인 [" + station.stationName() + "]으로 자동 설정되었습니다." : null);
    }

    private StationMappingResponse out(String input, String message) {
        return new StationMappingResponse(input, null, false, null, true, message);
    }

    private static StationRule rule(String id, String name, String... keywords) {
        return new StationRule(id, name, List.of(keywords));
    }

    private record StationRule(String stationId, String stationName, List<String> keywords) { }
}
