-- V4__alter_soft_delete_and_user_audit_tables.sql
-- 사용자/설비 기준정보 소프트 삭제 정책과 사용자 감사 로그, 비밀번호 재설정 토큰 테이블을 반영한다.

ALTER TABLE `user`
    ADD COLUMN account_status ENUM('ACTIVE', 'DELETED') NOT NULL DEFAULT 'ACTIVE' AFTER role,
    ADD COLUMN updated_by BIGINT NULL AFTER created_by,
    ADD COLUMN deleted_by BIGINT NULL AFTER updated_by,
    ADD COLUMN restored_at DATETIME NULL AFTER deleted_by,
    ADD COLUMN restored_by BIGINT NULL AFTER restored_at,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at,
    ADD CONSTRAINT fk_user_updated_by FOREIGN KEY (updated_by)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT fk_user_deleted_by FOREIGN KEY (deleted_by)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    ADD CONSTRAINT fk_user_restored_by FOREIGN KEY (restored_by)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

ALTER TABLE site
    ADD COLUMN updated_at DATETIME NULL AFTER created_at,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;

ALTER TABLE panel
    ADD COLUMN updated_at DATETIME NULL AFTER fire_threshold,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;

ALTER TABLE circuit
    ADD COLUMN updated_at DATETIME NULL AFTER created_at,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;

ALTER TABLE user_site
    ADD COLUMN updated_at DATETIME NULL AFTER assigned_at,
    ADD COLUMN deleted_at DATETIME NULL AFTER updated_at;

ALTER TABLE refresh_token
    DROP FOREIGN KEY fk_refresh_token_user;

ALTER TABLE refresh_token
    ADD CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT;

CREATE TABLE password_reset_token (
    token_id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at DATETIME NOT NULL,
    used_at DATETIME NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    request_ip VARCHAR(45) NULL,
    user_agent VARCHAR(255) NULL,
    PRIMARY KEY (token_id),
    CONSTRAINT uk_password_reset_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_password_reset_token_user FOREIGN KEY (user_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE user_audit_log (
    audit_id BIGINT NOT NULL AUTO_INCREMENT,
    target_user_id BIGINT NOT NULL,
    actor_user_id BIGINT NULL,
    action ENUM('CREATE', 'UPDATE', 'DELETE', 'RESTORE', 'PASSWORD_RESET') NOT NULL,
    before_data JSON NULL,
    after_data JSON NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id),
    CONSTRAINT fk_user_audit_log_target_user FOREIGN KEY (target_user_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_user_audit_log_actor_user FOREIGN KEY (actor_user_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
