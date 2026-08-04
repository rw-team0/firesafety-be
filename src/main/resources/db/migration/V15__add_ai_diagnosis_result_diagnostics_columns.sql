-- V15__add_ai_diagnosis_result_diagnostics_columns.sql
-- AI 서버가 판정마다 이미 돌려주던 n_samples/warning을 저장만 안 하고 버리고 있었다. REQ-103 화면에서
-- "이 판정이 몇 개 샘플로 나온 건지" 보여주려면 필요해서 저장 대상에 추가한다.

ALTER TABLE ai_diagnosis_result
    ADD COLUMN n_samples INT NULL AFTER confidence,
    ADD COLUMN warning VARCHAR(255) NULL AFTER n_samples;
