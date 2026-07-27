-- V13__add_alert_resolved_by.sql
-- 조치완료(RESOLVED) 처리자를 확인(CONFIRMED) 처리자(confirmed_by)와 동일하게 기록한다.

ALTER TABLE alert
    ADD COLUMN resolved_by BIGINT NULL AFTER resolved_at,
    ADD CONSTRAINT fk_alert_resolved_by FOREIGN KEY (resolved_by) REFERENCES `user` (user_id);
