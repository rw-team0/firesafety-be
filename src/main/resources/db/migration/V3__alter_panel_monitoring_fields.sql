-- V3__alter_panel_monitoring_fields.sql
-- 설비관리 최신 명세 반영: 장비명, 설치일, 회로 개수, 분전반별 임계치, OFFLINE 상태 추가
-- AI 수치예측은 제외되어 predicted_next_current 컬럼을 제거한다.

ALTER TABLE panel
    ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '분전반' AFTER site_id,
    ADD COLUMN installed_at DATE NULL AFTER m_no,
    MODIFY COLUMN status ENUM('NORMAL', 'CAUTION', 'RISK', 'OFFLINE') NOT NULL DEFAULT 'NORMAL',
    ADD COLUMN circuit_count INT NOT NULL DEFAULT 10 AFTER last_communicated_at,
    ADD COLUMN leak_ma_threshold DECIMAL(4,1) NOT NULL DEFAULT 20.0 AFTER circuit_count,
    ADD COLUMN temp_threshold DECIMAL(4,1) NOT NULL DEFAULT 80.0 AFTER leak_ma_threshold,
    ADD COLUMN humidity_threshold DECIMAL(4,1) NOT NULL DEFAULT 80.0 AFTER temp_threshold,
    ADD COLUMN overcurrent_threshold DECIMAL(4,1) NOT NULL DEFAULT 30.0 AFTER humidity_threshold,
    ADD COLUMN gas_threshold INT NULL AFTER overcurrent_threshold,
    ADD COLUMN fire_threshold INT NULL AFTER gas_threshold,
    ADD CONSTRAINT chk_panel_circuit_count CHECK (circuit_count BETWEEN 1 AND 10);

ALTER TABLE sensor_frame_circuit
    DROP COLUMN predicted_next_current;
