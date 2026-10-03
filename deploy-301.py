#!/usr/bin/env python3
"""Upload the small sauna to its separate 301 directory without a system service."""
import argparse
import os
import re
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--data-port', type=int, default=4569, help='Порт сервера данных в 301 (по умолчанию 4569)')
    parser.add_argument('--web-port', type=int, default=8082, help='Порт веб-интерфейса (по умолчанию 8082)')
    parser.add_argument('--log-name', default=None, help='Имя PLC; без параметра сохраняется встроенное значение')
    parser.add_argument('--build', action='store_true', help='Заново собрать JAR с серверными тестами')
    parser.add_argument('--ask-password', action='store_true', help='Вход на 301 по SSH-паролю вместо ключа')
    parser.add_argument('--check', action='store_true', help='Только проверить SSH, без загрузки и перезапуска')
    args = parser.parse_args()
    if not all(1 <= port <= 65535 for port in (args.data_port, args.web_port)):
        parser.error('Порты должны быть от 1 до 65535')
    if args.log_name is not None and not re.fullmatch(r'[A-Za-z0-9_.-]{0,64}', args.log_name):
        parser.error('Некорректное имя PLC')
    if args.check and args.build:
        parser.error('--check нельзя совмещать с --build')
    if args.ask_password and not sys.stdin.isatty():
        parser.error('--ask-password запускается в обычном терминале для безопасного ввода пароля')
    auth_options = (['-o', 'BatchMode=no', '-o', 'PubkeyAuthentication=no',
                     '-o', 'PreferredAuthentications=keyboard-interactive,password']
                    if args.ask_password else ['-o', 'BatchMode=yes', '-i', str(Path.home()/'.ssh/deploy_key')])
    if args.check:
        return subprocess.call(['ssh'] + auth_options + ['-o', 'ConnectTimeout=10',
                                '-o', 'StrictHostKeyChecking=yes',
                                'webpanel@172.16.6.119', 'true'])
    jar = ROOT/'backend/target/sauna.jar'
    if not args.build and not jar.is_file():
        parser.error('Готовый sauna.jar не найден. Добавь --build для сборки.')
    env = os.environ.copy()
    env.update(SERVER_USER='webpanel', SERVER_IP='172.16.6.119',
               SSH_KEY=str(Path.home()/'.ssh/deploy_key'),
               SSH_PASSWORD_AUTH='1' if args.ask_password else '0',
               REMOTE_DEST_DIR='/opt/webpanel/sauna', WEB_PORT=str(args.web_port),
               MODBUS_API_IP='127.0.0.1', MODBUS_API_PORT=str(args.data_port),
               SKIP_BUILD='0' if args.build else '1')
    env.pop('MODBUS_LOG_NAME', None)
    if args.log_name is not None:
        env['MODBUS_LOG_NAME'] = args.log_name
    print('301: /opt/webpanel/sauna; веб %s; данные 127.0.0.1:%s; logName=%s.'
          % (args.web_port, args.data_port, repr(args.log_name) if args.log_name is not None else 'встроенное'), flush=True)
    return subprocess.call(['bash', str(ROOT/'tools/deploy_301.sh')], cwd=ROOT, env=env)


if __name__ == '__main__':
    try:
        sys.exit(main())
    except KeyboardInterrupt:
        print('Прервано. Если установка уже началась, проверь состояние приложения перед повтором.', file=sys.stderr)
        sys.exit(130)
    except OSError as error:
        print('Не удалось запустить установщик: ' + str(error), file=sys.stderr)
        sys.exit(1)
