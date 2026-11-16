#!/usr/bin/env bash
set -euo pipefail

CONTAINER_NAME="${DB_CONTAINER_NAME:-kaikan}"
DB_USER="${DB_USER:-kaikan}"
DB_NAME="${DB_NAME:-kaikan}"

docker exec -i "$CONTAINER_NAME" psql -U "$DB_USER" -d "$DB_NAME" < "$(dirname "$0")/carga_local.sql"
