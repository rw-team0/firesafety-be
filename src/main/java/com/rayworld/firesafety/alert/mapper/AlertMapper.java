package com.rayworld.firesafety.alert.mapper;

import com.rayworld.firesafety.alert.model.Alert;
import org.apache.ibatis.annotations.Mapper;

// 경보(alert) 생성/조회 및 상태 전이용 MyBatis Mapper
@Mapper
public interface AlertMapper {

    // 경보 발생 이력 저장
    void insertAlert(Alert alert);
}
