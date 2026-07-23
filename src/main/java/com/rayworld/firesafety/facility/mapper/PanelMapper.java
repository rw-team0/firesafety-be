package com.rayworld.firesafety.facility.mapper;

import com.rayworld.firesafety.facility.model.Panel;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 분전반(panel) 등록/조회 및 상태 관리용 MyBatis Mapper
@Mapper
public interface PanelMapper {

    // 분전반 등록
    void insertPanel(Panel panel);

    // 등록 직후 생성된 분전반 조회
    Panel findActivePanelById(@Param("panelId") Long panelId);

    // 장비 시리얼 중복 확인
    boolean existsPanelByDeviceSerial(@Param("deviceSerial") String deviceSerial);
}
