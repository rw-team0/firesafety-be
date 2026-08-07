-- 기존 경보는 하드웨어/AI/SYSTEM 위험 알림으로 운영되어 왔으므로 RISK로 보정하고,
-- 신규 주의 알림부터 CAUTION으로 저장한다.
ALTER TABLE alert
    ADD COLUMN severity ENUM('CAUTION','RISK') NOT NULL DEFAULT 'RISK' AFTER type;
