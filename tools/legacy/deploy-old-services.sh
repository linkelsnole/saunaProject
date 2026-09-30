#!/bin/bash

SERVER_USER="webpanel"
SERVER_IP="212.164.86.237"
JAR_NAME="sauna.jar"
LOCAL_JAR="backend/target/sauna.jar"
SSH_KEY="$HOME/.ssh/deploy_key"

# !!! ВАЖНО: Путь, откуда Systemd запускает файл
REMOTE_DEST_DIR="/opt/webpanel"

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m'

echo -e "${YELLOW}🚀 Deploy started (Small Sauna - Port 8080)...${NC}"

echo -e "${YELLOW}📦 Maven Build...${NC}"
cd backend
mvn clean package -DskipTests
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ Maven build failed${NC}"
    exit 1
fi
cd ..
echo -e "${GREEN}✅ Build success${NC}"

echo -e "${YELLOW}📤 Uploading new jar to ${REMOTE_DEST_DIR}...${NC}"
scp -i "$SSH_KEY" "$LOCAL_JAR" "${SERVER_USER}@${SERVER_IP}:${REMOTE_DEST_DIR}/${JAR_NAME}"
if [ $? -ne 0 ]; then
    echo -e "${RED}❌ SCP failed. Check permissions.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ File uploaded${NC}"

echo -e "${YELLOW}🔄 Triggering Systemd Restart (Kill process)...${NC}"
ssh -i "$SSH_KEY" "${SERVER_USER}@${SERVER_IP}" 'bash -s' << 'ENDSSH'
    # 1. Пробуем найти и убить по порту 8080
    PORT_PID=$(lsof -ti:8080 2>/dev/null)
    if [ -n "$PORT_PID" ]; then
        echo "Killing PID: $PORT_PID"
        kill $PORT_PID
    else
        echo "Process not found on port 8080 via lsof..."
    fi
    
    pkill -f "sauna.jar"
    
    echo "Waiting for Systemd to restart application..."
    
    # Ждем до 90 секунд
    for i in {1..45}; do
        sleep 2
        if lsof -ti:8080 > /dev/null 2>&1; then
            NEW_PID=$(lsof -ti:8080)
            echo "✅ Service restarted automatically! PID: $NEW_PID"
            exit 0
        fi
        echo -n "."
    done
    
    echo ""
    echo -e "⚠️ Timeout! Check logs at: /opt/webpanel/app_sauna.log"
    exit 1
ENDSSH

if [ $? -eq 0 ]; then
    echo -e "${GREEN}🎉 Deploy successful!${NC}"
else
    echo -e "${RED}❌ Service did not start correctly${NC}"
    exit 1
fi