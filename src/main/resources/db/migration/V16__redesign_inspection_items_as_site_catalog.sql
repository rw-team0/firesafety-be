-- V16__redesign_inspection_items_as_site_catalog.sql
-- 점검 항목을 분전반별 개별 등록에서 현장 공용 카탈로그로 재설계한다.
-- 이전 구조는 같은 항목을 분전반마다 반복 등록해야 했다. 이제 항목은 현장(site) 소속으로 한 번만
-- 등록하고, 분전반은 panel_inspection_item으로 카탈로그에서 원하는 항목만 골라 적용한다.
-- 이 기능은 아직 실사용 데이터가 없어(REQ-511/512 신규) 기존 테이블을 새로 만든다.

DROP TABLE IF EXISTS inspection_result_item;
DROP TABLE IF EXISTS inspection_result;
DROP TABLE IF EXISTS inspection_item;

CREATE TABLE inspection_item (
    item_id BIGINT NOT NULL AUTO_INCREMENT,
    site_id BIGINT NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    description VARCHAR(255) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id),
    CONSTRAINT fk_inspection_item_site FOREIGN KEY (site_id)
        REFERENCES site (site_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 분전반에 적용된(선택된) 점검 항목. 적용/해제는 항상 전체교체(delete-then-insert)로 처리한다.
CREATE TABLE panel_inspection_item (
    panel_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (panel_id, item_id),
    CONSTRAINT fk_panel_inspection_item_panel FOREIGN KEY (panel_id)
        REFERENCES panel (panel_id)
        ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT fk_panel_inspection_item_item FOREIGN KEY (item_id)
        REFERENCES inspection_item (item_id)
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
CREATE INDEX idx_inspection_item_site_id ON inspection_item (site_id);
CREATE INDEX idx_panel_inspection_item_item_id ON panel_inspection_item (item_id);
CREATE INDEX idx_inspection_result_panel_inspected ON inspection_result (panel_id, inspected_at);
CREATE INDEX idx_inspection_result_item_inspection_id ON inspection_result_item (inspection_id);
