-- 배포 시 생성하는 tar 아카이브를 버전 단위로 기록. SW 버전 정보 화면(SCR-702) 업데이트 이력이 이 테이블을 조회한다.
-- DB 스키마 변경 없이 소스코드만 바뀌는 배포도 많아 flyway_schema_history로는 커버가 안 돼서 별도로 둔다.
CREATE TABLE system_release_history (
    release_id BIGINT NOT NULL AUTO_INCREMENT,
    version VARCHAR(50) NOT NULL,
    description VARCHAR(255) NULL,
    released_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (release_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_system_release_history_released_at ON system_release_history (released_at);
