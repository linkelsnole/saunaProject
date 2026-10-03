#!/bin/bash
# Run from Terminal on the Mac with VPN access to 301.
set -euo pipefail
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$PROJECT_DIR"
printf '%s\n' 'Малая сауна → 301 (172.16.6.119)' 'PLC: HomePLC, порт данных: 4569, веб: 8082.' 'Введите SSH-пароль webpanel при запросе. Символы пароля не отображаются.'
exec python3 "$PROJECT_DIR/deploy-301.py" --ask-password --log-name HomePLC --data-port 4569 --web-port 8082 "$@"
