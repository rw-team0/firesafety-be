-- V14__create_inspection_tables.sql
-- 설비 점검 관리(REQ-511/512): 분전반별 점검 항목 정의, 점검 실행, 항목별 결과를 저장한다.

CREATE TABLE inspection_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    panel_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_inspection_item_panel FOREIGN KEY (panel_id)
        REFERENCES panel (panel_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inspection_result (
    inspection_id BIGINT NOT NULL AUTO_INCREMENT,
    panel_id BIGINT NOT NULL,
    inspected_at DATETIME NOT NULL,
    inspector_id BIGINT NOT NULL,
    note VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (inspection_id),
    CONSTRAINT fk_inspection_result_panel FOREIGN KEY (panel_id)
        REFERENCES panel (panel_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_inspection_result_inspector FOREIGN KEY (inspector_id)
        REFERENCES `user` (user_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE inspection_result_item (
    result_id BIGINT NOT NULL AUTO_INCREMENT,
    inspection_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    result ENUM('NORMAL', 'ABNORMAL', 'UNCHECKED') NOT NULL DEFAULT 'UNCHECKED',
    PRIMARY KEY (result_id),
    CONSTRAINT uk_inspection_result_item UNIQUE (inspection_id, item_id),
    CONSTRAINT fk_inspection_result_item_inspection FOREIGN KEY (inspection_id)
        REFERENCES inspection_result (inspection_id)
        ON UPDATE RESTRICT ON DELETE CASCADE,
    CONSTRAINT fk_inspection_result_item_item FOREIGN KEY (item_id)
        REFERENCES inspection_item (item_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 주요 조회 경로 인덱스
CREATE INDEX idx_inspection_item_panel_id ON inspection_item (panel_id);
CREATE INDEX idx_inspection_result_panel_inspected ON inspection_result (panel_id, inspected_at);
CREATE INDEX idx_inspection_result_item_inspection_id ON inspection_result_item (inspection_id);
