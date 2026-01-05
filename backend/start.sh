#!/bin/bash

echo "🔍 Проверяем порт 8080..."

PID=$(lsof -ti :8080)

if [ ! -z "$PID" ]; then
    echo "Найден процесс $PID на порту 8080. Завершаем..."
    kill -9 $PID
    sleep 2
    echo "Процесс завершен"
else
    echo "Порт 8080 свободен"
fi

echo "Запускаем приложение..."
mvn spring-boot:run
