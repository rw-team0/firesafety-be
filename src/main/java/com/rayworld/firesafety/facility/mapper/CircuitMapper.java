package com.rayworld.firesafety.facility.mapper;

import com.rayworld.firesafety.facility.model.Circuit;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

// 회로(circuit) 등록/조회 및 채널 중복 검사용 MyBatis Mapper
@Mapper
public interface CircuitMapper {

    // 회로 등록
    void insertCircuit(Circuit circuit);

    // 등록 직후 생성된 회로 조회
    Circuit findActiveCircuitById(@Param("circuitId") Long circuitId);

    // 분전반 안의 회로 번호 중복 확인
    boolean existsCircuitByPanelIdAndChannelNo(@Param("panelId") Long panelId, @Param("channelNo") Integer channelNo);
}
