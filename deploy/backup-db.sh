#!/usr/bin/env bash
# DB를 덤프해 S3 backup/에 올립니다. 오래된 덤프는 S3 수명 주기 규칙이 지웁니다.
# 접속 권한은 EC2 IAM 역할을 씁니다. DB 비밀번호는 명령줄에 쓰지 않고 설정 파일에서 읽습니다.
set -euo pipefail

: "${S3_BUCKET:?S3_BUCKET이 필요합니다}"
DB_NAME="${DB_NAME:-smartrent}"
MYSQL_DEFAULTS_FILE="${MYSQL_DEFAULTS_FILE:-/etc/smartrent/backup.cnf}"

name="smartrent-$(TZ=Asia/Seoul date +%Y-%m-%d_%H%M%S).sql.gz"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

# 서버 한 대라 GTID는 넣지 않습니다(새 DB에 복원할 수 있게). 마스킹 정책은 쓰지 않습니다.
mysqldump --defaults-extra-file="$MYSQL_DEFAULTS_FILE" \
	--single-transaction --no-tablespaces --routines --triggers \
	--set-gtid-purged=OFF --skip-masking-policies \
	"$DB_NAME" | gzip > "$tmp"
gzip -t "$tmp"

aws s3 cp "$tmp" "s3://${S3_BUCKET}/backup/${name}" --only-show-errors
echo "백업 완료: backup/${name}"
