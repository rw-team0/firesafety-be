-- V6__extend_alert_type_for_device_alarm_bits.sql
-- 하드웨어 aerror ALARM/ERROR bit 전체를 경보 타입으로 기록할 수 있게 확장한다.

ALTER TABLE alert
    MODIFY COLUMN type ENUM(
        'ARC',
        'OVERHEAT',
        'LEAKAGE',
        'OVERCURRENT',
        'HUMIDITY',
        'GAS',
        'FIRE',
        'DOOR_OPEN',
        'DEVICE_ERROR',
        'COMM_LOST'
    ) NOT NULL;
