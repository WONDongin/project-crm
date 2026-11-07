-- MySQL 8: 애플리케이션 실행 전 수동 적용 (ddl-auto=none).
-- 1. 아래 조회 결과가 있으면 중복 고객을 먼저 정리한다. 자동 병합/삭제하지 않는다.
SELECT REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phone, '-', ''), ' ', ''), '(', ''), ')', ''), '.', ''), '+', '') AS normalized_phone,
       COALESCE(birth_date, '1000-01-01') AS birth_identity, COUNT(*) AS duplicate_count
FROM customers GROUP BY normalized_phone, birth_identity HAVING COUNT(*) > 1;

-- 2. 서식이 다른 연락처도 동일한 고객 식별값으로 비교한다.
-- 기존 값은 유지하며 새 등록/수정만 숫자로 정규화한다.
ALTER TABLE customers
    ADD COLUMN phone_identity VARCHAR(20) GENERATED ALWAYS AS
        (REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(phone, '-', ''), ' ', ''), '(', ''), ')', ''), '.', ''), '+', '')) STORED,
    ADD COLUMN birth_identity DATE GENERATED ALWAYS AS (COALESCE(birth_date, '1000-01-01')) STORED,
    ADD CONSTRAINT uk_customer_phone_birth UNIQUE (phone_identity, birth_identity);

-- 첨부파일은 기존 file_mng와 activity_logs.file_upload_no를 사용한다.
-- 신규 첨부 테이블 / BLOB 컬럼 / max_allowed_packet 변경은 필요하지 않다.
-- 기존 file_mng의 파일 그룹 번호와 데이터는 그대로 유지한다.
