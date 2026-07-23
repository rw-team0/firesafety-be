-- V7__add_panel_created_at_column.sql
-- panel은 기준정보 테이블이므로 site/circuit처럼 생성일시를 함께 관리한다.

ALTER TABLE panel
    ADD COLUMN created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP AFTER fire_threshold;
