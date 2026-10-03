#!/usr/bin/env bash
set -Eeuo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

SERVER_USER="${SERVER_USER:-webpanel}"
SERVER_IP="${SERVER_IP:-172.16.6.119}"
SSH_KEY="${SSH_KEY:-${HOME}/.ssh/deploy_key}"

REMOTE_DEST_DIR="${REMOTE_DEST_DIR:-/opt/webpanel/sauna}"
JAR_NAME="sauna.jar"
LOCAL_JAR="${PROJECT_DIR}/backend/target/${JAR_NAME}"
WEB_PORT="${WEB_PORT:-8082}"
MODBUS_API_IP="${MODBUS_API_IP:-172.16.6.119}"
MODBUS_API_PORT="${MODBUS_API_PORT:-4571}"
MODBUS_LOG_NAME_SET="${MODBUS_LOG_NAME+x}"
MODBUS_LOG_NAME="${MODBUS_LOG_NAME-}"
SKIP_BUILD="${SKIP_BUILD:-0}"
SSH_PASSWORD_AUTH="${SSH_PASSWORD_AUTH:-0}"
SSH_OPTIONS=(-o BatchMode=yes -o ConnectTimeout=10 -o StrictHostKeyChecking=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=2)
SSH_IDENTITY=(-i "${SSH_KEY}")
if [[ "${SSH_PASSWORD_AUTH}" == "1" ]]; then
  SSH_OPTIONS=(-o BatchMode=no -o PubkeyAuthentication=no -o PreferredAuthentications=keyboard-interactive,password -o ConnectTimeout=10 -o StrictHostKeyChecking=yes -o ServerAliveInterval=15 -o ServerAliveCountMax=2)
  SSH_IDENTITY=(-o NumberOfPasswordPrompts=3)
fi
DATABASE_NAME="sauna_app.db"

DEFAULT_JAVA_8_HOME="/Users/dmitry/Library/Java/JavaVirtualMachines/corretto-1.8.0_482/Contents/Home"
JAVA_8_HOME="${JAVA_8_HOME:-${DEFAULT_JAVA_8_HOME}}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Не найдена обязательная команда: $1" >&2
    exit 1
  fi
}

require_command npm
require_command mvn
require_command scp
require_command ssh

if [[ ! -x "${JAVA_8_HOME}/bin/java" ]]; then
  echo "Java 8 не найдена: ${JAVA_8_HOME}" >&2
  echo "Укажи путь через JAVA_8_HOME=/path/to/java8" >&2
  exit 1
fi
if [[ "${SSH_PASSWORD_AUTH}" != "1" && ! -f "${SSH_KEY}" ]]; then
  echo "SSH-ключ не найден: ${SSH_KEY}" >&2
  exit 1
fi

export JAVA_HOME="${JAVA_8_HOME}"
export PATH="${JAVA_HOME}/bin:${PATH}"

REMOTE_HOST="${SERVER_USER}@${SERVER_IP}"
echo "Проверка SSH к ${REMOTE_HOST}..."
ssh "${SSH_OPTIONS[@]}" "${SSH_IDENTITY[@]}" "${REMOTE_HOST}" true

if [[ "${SKIP_BUILD}" != "1" ]]; then
echo "Сборка sauna..."
(
  cd "${PROJECT_DIR}"
  npm run build
  mvn -f backend/pom.xml clean package -Dskip.frontend.build=true
)

fi

if [[ ! -f "${LOCAL_JAR}" ]]; then
  echo "Jar после сборки не найден: ${LOCAL_JAR}" >&2
  exit 1
fi

REMOTE_HOST="${SERVER_USER}@${SERVER_IP}"
echo "Подготовка ${REMOTE_HOST}:${REMOTE_DEST_DIR}..."
printf -v PREPARE_COMMAND 'bash -s -- %q' "${REMOTE_DEST_DIR}"
ssh "${SSH_OPTIONS[@]}" "${SSH_IDENTITY[@]}" "${REMOTE_HOST}" "${PREPARE_COMMAND}" <<'ENDSSH'
set -Eeuo pipefail
mkdir -p "$1"
ENDSSH

echo "Загрузка ${JAR_NAME}..."
scp "${SSH_OPTIONS[@]}" "${SSH_IDENTITY[@]}" "${LOCAL_JAR}" "${REMOTE_HOST}:${REMOTE_DEST_DIR}/${JAR_NAME}.new"

echo "Перезапуск sauna на порту ${WEB_PORT}..."
printf -v RESTART_COMMAND 'bash -s -- %q %q %q %q %q %q %q %q' \
  "${REMOTE_DEST_DIR}" "${JAR_NAME}" "${WEB_PORT}" \
  "${MODBUS_API_IP}" "${MODBUS_API_PORT}" "${DATABASE_NAME}" "${MODBUS_LOG_NAME}" "${MODBUS_LOG_NAME_SET}"
ssh "${SSH_OPTIONS[@]}" "${SSH_IDENTITY[@]}" "${REMOTE_HOST}" "${RESTART_COMMAND}" <<'ENDSSH'
set -Eeuo pipefail

APP_DIR="$1"
JAR_NAME="$2"
WEB_PORT="$3"
MODBUS_API_IP="$4"
MODBUS_API_PORT="$5"
DATABASE_NAME="$6"
MODBUS_LOG_NAME="$7"
LOG_NAME_ARGS=()
if [[ "$8" == "x" ]]; then
  LOG_NAME_ARGS=("--modbus.api.log-name=${MODBUS_LOG_NAME}")
fi
PID_FILE="${APP_DIR}/app.pid"
LOG_FILE="${APP_DIR}/app.log"

OLD_PID=""
if [[ -f "${PID_FILE}" ]]; then
  OLD_PID="$(tr -d '[:space:]' < "${PID_FILE}")"
fi

if [[ "${OLD_PID}" =~ ^[0-9]+$ ]] && kill -0 "${OLD_PID}" 2>/dev/null; then
  OLD_COMMAND="$(ps -p "${OLD_PID}" -o command= 2>/dev/null || true)"
  if [[ "${OLD_COMMAND}" == *"${APP_DIR}/${JAR_NAME}"* ]]; then
    kill "${OLD_PID}"
    for _ in $(seq 1 20); do
      if ! kill -0 "${OLD_PID}" 2>/dev/null; then
        break
      fi
      sleep 1
    done
    if kill -0 "${OLD_PID}" 2>/dev/null; then
      echo "Старый процесс ${OLD_PID} не остановился" >&2
      exit 1
    fi
  else
    echo "PID-файл указывает на посторонний процесс ${OLD_PID}; процесс не тронут" >&2
    exit 1
  fi
fi

if command -v lsof >/dev/null 2>&1; then
  PORT_PID="$(lsof -tiTCP:"${WEB_PORT}" -sTCP:LISTEN 2>/dev/null | head -n 1 || true)"
  if [[ -n "${PORT_PID}" ]]; then
    echo "Порт ${WEB_PORT} уже занят процессом ${PORT_PID}; процесс не тронут" >&2
    exit 1
  fi
fi

if [[ -f "${APP_DIR}/${JAR_NAME}" ]]; then
  cp -p "${APP_DIR}/${JAR_NAME}" "${APP_DIR}/${JAR_NAME}.backup-$(date +%Y%m%d-%H%M%S)"
fi
mv "${APP_DIR}/${JAR_NAME}.new" "${APP_DIR}/${JAR_NAME}"
cd "${APP_DIR}"

nohup java -jar "${APP_DIR}/${JAR_NAME}" \
  --spring.profiles.active=prod \
  --server.address=0.0.0.0 \
  --server.port="${WEB_PORT}" \
  --modbus.api.ip="${MODBUS_API_IP}" \
  --modbus.api.port="${MODBUS_API_PORT}" \
  "${LOG_NAME_ARGS[@]}" \
  "--spring.datasource.url=jdbc:sqlite:${APP_DIR}/${DATABASE_NAME}?busy_timeout=10000" \
  > "${LOG_FILE}" 2>&1 < /dev/null &

NEW_PID=$!
printf '%s\n' "${NEW_PID}" > "${PID_FILE}"

for _ in $(seq 1 60); do
  if ! kill -0 "${NEW_PID}" 2>/dev/null; then
    echo "Приложение завершилось во время запуска" >&2
    tail -n 40 "${LOG_FILE}" >&2 || true
    exit 1
  fi
  if grep -Fq "Started ModbusApiApplication" "${LOG_FILE}"; then
    echo "sauna запущена: PID=${NEW_PID}, http://172.16.6.119:${WEB_PORT}"
    exit 0
  fi
  sleep 1
done

echo "Приложение работает, но не подтвердило полный старт за 60 секунд" >&2
tail -n 40 "${LOG_FILE}" >&2 || true
exit 1
ENDSSH

echo "Deploy sauna завершён успешно."
