-- V19__add_site_address_detail.sql
-- 현장 등록/수정 화면에 도로명주소 검색 결과 외 상세주소(동/호수 등) 입력칸이 없었다.
-- 상세주소는 검색 결과에 없는 자유 입력값이라 선택 입력으로 둔다.

ALTER TABLE site
    ADD COLUMN address_detail VARCHAR(200) NULL AFTER address;
