-- SW 버전 정보 화면에서 소프트웨어 릴리즈와 AI 모델 버전을 같은 이력 테이블에서 구분 관리하도록 확장.
-- updated_by는 로그인 계정이 아니라 등록 모달에서 직접 입력하는 자유 텍스트(실제 배포/학습 담당자 이름이 계정과 다를 수 있음).
ALTER TABLE system_release_history
    ADD COLUMN type ENUM('SOFTWARE', 'MODEL') NOT NULL DEFAULT 'SOFTWARE' AFTER version,
    ADD COLUMN updated_by VARCHAR(100) NULL AFTER description;
