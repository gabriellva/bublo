#!/usr/bin/env bash
# Smoke test: logs in, creates data through the real forms and requests every page.
# Catches template errors and SQL mapping errors that unit tests cannot see.
# Usage: BASE_URL=http://localhost:8090 ADMIN_USER=admin ADMIN_PASSWORD=... scripts/smoke-test.sh
set -u

BASE_URL="${BASE_URL:-http://localhost:8080}"
ADMIN_USER="${ADMIN_USER:-admin}"
ADMIN_PASSWORD="${ADMIN_PASSWORD:?Set ADMIN_PASSWORD}"
JAR="$(mktemp)"
trap 'rm -f "$JAR"' EXIT
FAILURES=0

csrf_from() {
  curl -s -b "$JAR" -c "$JAR" "$BASE_URL$1" | grep -o 'name="_csrf" \(content\|value\)="[^"]*"' | head -1 | sed 's/.*="//;s/"$//'
}

check_status() {
  local label=$1 expected=$2 actual=$3
  if [ "$actual" = "$expected" ]; then
    printf 'ok    %-45s %s\n' "$label" "$actual"
    return
  fi
  printf 'FAIL  %-45s expected %s, got %s\n' "$label" "$expected" "$actual"
  FAILURES=$((FAILURES + 1))
}

post_form() {
  local path=$1; shift
  local token; token=$(csrf_from /conta)
  curl -s -b "$JAR" -c "$JAR" -o /dev/null -w '%{http_code}' --data-urlencode "_csrf=$token" "$@" "$BASE_URL$path"
}

token=$(csrf_from /login)
status=$(curl -s -b "$JAR" -c "$JAR" -o /dev/null -w '%{http_code}' --data-urlencode "_csrf=$token" \
  --data-urlencode "username=$ADMIN_USER" --data-urlencode "password=$ADMIN_PASSWORD" "$BASE_URL/login")
check_status "login" 302 "$status"

check_status "POST without CSRF token is rejected" 403 \
  "$(curl -s -b "$JAR" -o /dev/null -w '%{http_code}' -X POST "$BASE_URL/emprestimos/1/cancelar")"

reader_id=$(curl -s -b "$JAR" "$BASE_URL/emprestimos/novo" | grep -o '<option value="[0-9]*" data-tipo' | head -1 | grep -o '[0-9]*')
copy_id=$(curl -s -b "$JAR" "$BASE_URL/emprestimos/novo" | grep -o '<option value="[0-9]*"[^>]*>BA-' | head -1 | grep -o 'value="[0-9]*"' | grep -o '[0-9]*')
due_date=$(date -d '+7 days' +%F)

check_status "create loan (also creates a notification)" 302 \
  "$(post_form /emprestimos -d "leitorId=$reader_id" -d "exemplares=$copy_id" -d "dataPrevista=$due_date")"

PAGES="/dashboard /acervo /acervo/novo /acervo/1 /acervo/1/editar /leitores /leitores/novo /leitores/1 /leitores/1/editar
/emprestimos /emprestimos/novo /devolucoes /reservas /reservas/novo /multas /relatorios /configuracoes /auditoria
/notificacoes /notificacoes/resumo /conta /relatorios/emprestimos.csv /relatorios/emprestimos.xlsx /relatorios/multas.csv
/relatorios/multas.xlsx /relatorios/leitores.csv /relatorios/reservas.csv /relatorios/materiais.csv /leitores/modelo-csv
/emprestimos/restricao/1 /acervo/sugestoes-autores?termo=a"
for page in $PAGES; do
  check_status "GET $page" 200 "$(curl -s -b "$JAR" -o /dev/null -w '%{http_code}' "$BASE_URL$page")"
done

check_status "GET unknown page renders the error page" 404 \
  "$(curl -s -b "$JAR" -o /dev/null -w '%{http_code}' "$BASE_URL/nao-existe")"
check_status "ORDER BY injection attempt is ignored" 200 \
  "$(curl -s -b "$JAR" -o /dev/null -w '%{http_code}' "$BASE_URL/leitores?ordenacao=(select%20pg_sleep(1))")"
check_status "GET on a mutation endpoint is not allowed" 405 \
  "$(curl -s -b "$JAR" -o /dev/null -w '%{http_code}' "$BASE_URL/emprestimos/1/cancelar")"

if [ "$FAILURES" -gt 0 ]; then
  echo "$FAILURES check(s) failed"
  exit 1
fi
echo "All checks passed"
