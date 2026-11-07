-- MySQL 8: 기존 customers(phone, birth_date) 복합 UNIQUE와 birth_date NOT NULL 사용.
-- phone_identity / birth_identity 컬럼과 추가 UNIQUE 인덱스는 만들지 않는다.
-- 아래 SELECT는 확인용이다. 고객 정보의 자동 병합/삭제/변경은 수행하지 않는다.

-- 1. 숫자만 남겼을 때 동일한 연락처 + 생년월일이 되는 고객 확인.
-- 결과가 있으면 해당 고객을 검토하고 중복을 해소한 뒤 기존 연락처를 정리한다.
SELECT REGEXP_REPLACE(phone, '[^0-9]', '') AS normalized_phone,
       birth_date, COUNT(*) AS duplicate_count
FROM customers
GROUP BY normalized_phone, birth_date
HAVING COUNT(*) > 1;

-- 2. 정규화 후에도 서비스의 숫자 9~11자리 규칙을 만족하지 않는 데이터 확인.
SELECT customer_id, phone, birth_date
FROM customers
WHERE phone IS NULL
   OR birth_date IS NULL
   OR REGEXP_REPLACE(phone, '[^0-9]', '') NOT REGEXP '^[0-9]{9,11}$';

-- 3. 변경 대상 미리보기. 1, 2번 조회 결과가 없고 백업을 완료한 뒤에만 수동 정리한다.
SELECT customer_id, phone AS current_phone,
       REGEXP_REPLACE(phone, '[^0-9]', '') AS normalized_phone
FROM customers
WHERE phone <> REGEXP_REPLACE(phone, '[^0-9]', '');

-- 수동 적용 예시 (기본적으로 실행되지 않도록 주석 처리).
-- 정리 중에는 상담 신청/고객 수정 요청을 중지하여 사전 검사 이후의 변경을 방지한다.
-- START TRANSACTION;
-- UPDATE customers
-- SET phone = REGEXP_REPLACE(phone, '[^0-9]', '')
-- WHERE phone <> REGEXP_REPLACE(phone, '[^0-9]', '');
-- 변경 결과 및 중복 여부 확인 후 COMMIT, 오류가 있으면 ROLLBACK한다.
-- COMMIT;
-- ROLLBACK;

-- 신규 상담 신청과 고객 정보 수정은 서버에서 숫자만 남겨 저장한다.
-- 동시 저장 중복은 기존 customers(phone, birth_date) UNIQUE 제약으로 차단한다.
-- 첨부파일은 기존 file_mng와 activity_logs.file_upload_no를 사용한다.
-- 신규 첨부 테이블 / BLOB 컬럼 / max_allowed_packet 변경은 필요하지 않다.
-- 기존 file_mng의 파일 그룹 번호와 데이터는 그대로 유지한다.
