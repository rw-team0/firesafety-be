package com.rayworld.firesafety.inspection.mapper;

import com.rayworld.firesafety.inspection.dto.res.InspectionExportRowRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionHistoryRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionItemRes;
import com.rayworld.firesafety.inspection.dto.res.InspectionResultItemRes;
import com.rayworld.firesafety.inspection.model.InspectionItem;
import com.rayworld.firesafety.inspection.model.InspectionResult;
import com.rayworld.firesafety.inspection.model.InspectionResultItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

// 설비 점검 관리(REQ-511/512)용 MyBatis Mapper
@Mapper
public interface InspectionMapper {

    // 점검 항목 등록
    void insertInspectionItem(InspectionItem item);

    // 분전반의 점검 항목 목록 조회
    List<InspectionItemRes> findInspectionItemsByPanelId(@Param("panelId") Long panelId);

    // 점검 항목 존재 여부 확인 (같은 분전반 소속인지까지 함께 확인)
    boolean existsInspectionItem(@Param("itemId") Long itemId, @Param("panelId") Long panelId);

    // 점검 실행 1건 등록
    void insertInspectionResult(InspectionResult inspectionResult);

    // 점검 실행 1건에 딸린 항목별 결과 등록
    void insertInspectionResultItem(InspectionResultItem inspectionResultItem);

    // 분전반의 점검 이력 목록 조회 (항목별 결과는 별도 조회해서 조립)
    List<InspectionHistoryRes> findInspectionHistory(@Param("panelId") Long panelId,
                                                     @Param("fromAt") LocalDateTime fromAt,
                                                     @Param("toAt") LocalDateTime toAt,
                                                     @Param("size") int size,
                                                     @Param("offset") int offset);

    // 점검 이력 전체 개수 조회
    long countInspectionHistory(@Param("panelId") Long panelId,
                               @Param("fromAt") LocalDateTime fromAt,
                               @Param("toAt") LocalDateTime toAt);

    // 점검 실행 1건의 항목별 결과 조회 (항목명 포함)
    List<InspectionResultItemRes> findResultItemsByInspectionId(@Param("inspectionId") Long inspectionId);

    // 점검 이력 엑셀 다운로드용 항목별 행 조회
    List<InspectionExportRowRes> findInspectionExportRows(@Param("panelId") Long panelId,
                                                          @Param("fromAt") LocalDateTime fromAt,
                                                          @Param("toAt") LocalDateTime toAt);
}
