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

-- 3. 첨부 메타데이터와 본문을 같은 트랜잭션으로 저장/삭제한다.
CREATE TABLE activity_attachments (
    file_id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    activity_id BIGINT NOT NULL,
    original_name VARCHAR(200) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    uploaded_by BIGINT NOT NULL,
    uploaded_at DATETIME(6) NOT NULL,
    content LONGBLOB NOT NULL,
    CONSTRAINT fk_attachment_activity FOREIGN KEY (activity_id) REFERENCES activity_logs(activity_id),
    CONSTRAINT fk_attachment_uploader FOREIGN KEY (uploaded_by) REFERENCES users(user_id),
    CONSTRAINT chk_attachment_size CHECK (size_bytes > 0 AND size_bytes <= 10485760),
    INDEX idx_attachment_activity (activity_id, file_id)
);
-- max_allowed_packet은 최대 파일과 쿼리 오버헤드를 수용하도록 32MB 이상으로 설정한다.
