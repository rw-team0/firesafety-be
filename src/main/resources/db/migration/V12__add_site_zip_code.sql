-- V12__add_site_zip_code.sql
-- 현장 등록 시 도로명주소 검색(REQ-510) 결과에 포함된 우편번호를 함께 저장한다.

ALTER TABLE site
    ADD COLUMN zip_code VARCHAR(10) NULL AFTER address;
