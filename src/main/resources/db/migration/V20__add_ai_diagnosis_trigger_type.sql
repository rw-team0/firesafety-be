-- V20__add_ai_diagnosis_trigger_type.sql
-- 기존 진단 결과는 자동/수동 출처를 역추적할 수 없어 UNKNOWN으로 두고, 신규 결과부터 저장 시점에 출처를 기록한다.

ALTER TABLE ai_diagnosis_result
    ADD COLUMN trigger_type ENUM('AUTO','MANUAL','MOCK','UNKNOWN') NOT NULL DEFAULT 'UNKNOWN' AFTER warning;
