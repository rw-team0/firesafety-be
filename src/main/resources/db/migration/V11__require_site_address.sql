-- V11__require_site_address.sql
-- 현장 등록 시 주소 자동완성(공공데이터 연동, REQ-510) 도입에 맞춰 주소를 필수값으로 변경
-- 기존에 주소 없이 등록된 현장은 명시적으로 식별 가능한 placeholder로 백필한다.

UPDATE site SET address = '주소 미입력(관리자 확인 필요)' WHERE address IS NULL;

ALTER TABLE site
    MODIFY COLUMN address VARCHAR(200) NOT NULL;
