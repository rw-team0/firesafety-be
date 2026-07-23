package com.rayworld.firesafety.facility.mapper;

import com.rayworld.firesafety.facility.model.CircuitStatusRow;
import com.rayworld.firesafety.facility.dto.res.PanelRecentAlertRes;
import com.rayworld.firesafety.facility.model.Panel;
import com.rayworld.firesafety.sensor.model.SensorFrame;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

// 분전반(panel) 등록/조회 및 상태 관리용 MyBatis Mapper
@Mapper
public interface PanelMapper {

    // 분전반 등록
    void insertPanel(Panel panel);

    // 등록 직후 생성된 분전반 조회
    Panel findActivePanelById(@Param("panelId") Long panelId);

    // 디바이스 장비번호로 활성 분전반 조회
    Panel findActivePanelByMNo(@Param("mNo") String mNo);

    // SUPER_ADMIN 분전반 목록 조회
    List<Panel> findActivePanels(@Param("siteId") Long siteId, @Param("status") String status);

    // ADMIN/GENERAL 담당 현장 분전반 목록 조회
    List<Panel> findActivePanelsByUserId(@Param("userId") Long userId,
                                          @Param("siteId") Long siteId,
                                          @Param("status") String status);

    // 장비 시리얼 중복 확인
    boolean existsPanelByDeviceSerial(@Param("deviceSerial") String deviceSerial);

    // 수정 시 자기 자신을 제외한 장비 시리얼 중복 확인
    boolean existsPanelByDeviceSerialExceptSelf(@Param("panelId") Long panelId,
                                                @Param("deviceSerial") String deviceSerial);

    // 분전반 기본 정보와 임계치 수정
    int updatePanel(Panel panel);

    // 분전반 소프트 삭제
    int softDeletePanel(@Param("panelId") Long panelId);

    // 디바이스 수신 성공 시 마지막 통신시각과 온라인 상태 갱신
    int updatePanelCommunication(@Param("panelId") Long panelId);

    // 분전반 상세용 최신 센서 프레임 조회 (없으면 null)
    SensorFrame findLatestSensorFrameByPanelId(@Param("panelId") Long panelId);

    // 분전반 상세용 회로별 최신 전류/아크/AI 판정 원시값 조회
    List<CircuitStatusRow> findCircuitStatusRowsByPanelId(@Param("panelId") Long panelId);

    // 분전반 상세용 최근 경보 조회
    List<PanelRecentAlertRes> findRecentAlertsByPanelId(@Param("panelId") Long panelId, @Param("limit") int limit);
}
