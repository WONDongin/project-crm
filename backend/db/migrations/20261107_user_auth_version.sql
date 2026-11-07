-- 계정 상태/권한/이메일 변경 및 비밀번호 초기화 시 기존 JWT 폐기용 버전.
-- ddl-auto=none: 새 애플리케이션 실행 전에 MySQL에 한 번 수동 적용한다.
-- 이미 컬럼이 있는 환경에서는 실행하지 않는다.
ALTER TABLE users ADD COLUMN auth_version BIGINT NOT NULL DEFAULT 0 COMMENT '인증 토큰 버전';
-- 기존 토큰에는 사용자 ID/버전 claim이 없어 배포 후 다시 로그인해야 한다.
