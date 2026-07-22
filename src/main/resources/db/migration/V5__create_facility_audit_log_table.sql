-- V5__create_facility_audit_log_table.sql
-- 현장/분전반/회로 등록, 수정, 소프트 삭제 감사 로그를 통합 저장한다.

CREATE TABLE facility_audit_log (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    target_type ENUM('SITE', 'PANEL', 'CIRCUIT') NOT NULL,
    target_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    action ENUM('CREATE', 'UPDATE', 'DELETE') NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id),
    CONSTRAINT fk_facility_audit_log_actor_user FOREIGN KEY (actor_user_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_facility_audit_log_target ON facility_audit_log (target_type, target_id, created_at);
CREATE INDEX idx_facility_audit_log_actor ON facility_audit_log (actor_user_id, created_at);
CREATE INDEX idx_facility_audit_log_created_at ON facility_audit_log (created_at);
