# Малая сауна

React + TypeScript, Spring Boot 2.7 / Java 8. Один JAR содержит сервер и веб-интерфейс.

## Разработка

```bash
npm ci
npm run dev
```

Backend запускается отдельно на Java 8. Параметры реального контроллера передаются при запуске; для тестов VPN не нужен.

## Проверки и сборка

```bash
npm test
npm run build
python3 -m unittest discover -s tools -p 'test_*.py'
python3 -m unittest discover -s tools/deployment -p 'test_*.py'
mvn -B -f backend/pom.xml clean verify -Dskip.frontend.build=true
python3 tools/deployment/check_package.py
```

Артефакт: `backend/target/sauna.jar`.

## Инструкции

- [Обновление через GitHub и кнопку в веб-панели](docs/DEPLOYMENT.md)
- [Резервный ручной деплой](docs/DEPLOY-SMALL.md)
- [Расписание и пользователи](docs/SCHEDULE-AND-USERS.md)
- [Логи и ошибки](docs/LOGS-AND-ERRORS.md)

Рабочие базы SQLite, токены, логи, резервные копии и собранные файлы не коммитятся. `.gitignore` не удаляет их с компьютера.
