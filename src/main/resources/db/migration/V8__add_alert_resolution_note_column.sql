-- V8__add_alert_resolution_note_column.sql
-- 경보 조치완료 시 사용자가 입력한 처리 메모를 엑셀 비고 컬럼에 출력할 수 있게 저장한다.

ALTER TABLE alert
    ADD COLUMN resolution_note VARCHAR(500) NULL AFTER resolved_at;
