-- V10__create_user_fcm_token_table.sql
-- fcm_token 단일 컬럼은 사용자당 1개 기기만 지원해서, 여러 기기로 로그인하면
-- 최신 로그인 기기 토큰이 이전 기기 토큰을 덮어써 알림을 못 받는 문제가 있었다.
-- 계정당 여러 기기 토큰을 저장할 수 있도록 별도 테이블로 분리한다.

CREATE TABLE user_fcm_token (
    token_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    fcm_token VARCHAR(255) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_fcm_token_fcm_token (fcm_token),
    CONSTRAINT fk_user_fcm_token_user FOREIGN KEY (user_id) REFERENCES `user` (user_id)
);

-- 기존에 저장돼 있던 토큰이 있으면 새 테이블로 옮긴다
INSERT INTO user_fcm_token (user_id, fcm_token)
SELECT user_id, fcm_token FROM `user` WHERE fcm_token IS NOT NULL AND fcm_token != '';

ALTER TABLE `user` DROP COLUMN fcm_token;
