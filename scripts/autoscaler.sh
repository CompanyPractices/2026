#!/bin/bash

SERVICE_NAME="terminal-simulator"

RPS_PER_INSTANCE=200
MIN_CONTAINERS=1
MAX_CONTAINERS=10

echo "Автоскейлер запущен"

while true; do
  RESPONSE=$(curl -s "http://localhost:9090/api/v1/query?query=sum%28rate%28http_server_requests_seconds_count%7Bapplication%3D%22gateway%22%7D%5B1m%5D%29%29")

  RAW_RPS=$(echo "$RESPONSE" | sed -E 's/.*,\[.*,"([^"]+)".*/\1/')


  if [[ "$RAW_RPS" == *"{"* || -z "$RAW_RPS" ]]; then
    RAW_RPS="0"
  fi

  RPS=${RAW_RPS%.*}

  if [ -z "$RPS" ]; then RPS=$RAW_RPS; fi
  if ! [[ "$RPS" =~ ^[0-9]+$ ]]; then RPS=0; fi

  echo "Текущая нагрузка на Gateway: $RPS RPS"

  NEEDED_CONTAINERS=$(( (RPS + RPS_PER_INSTANCE - 1) / RPS_PER_INSTANCE ))

  if [ "$NEEDED_CONTAINERS" -lt "$MIN_CONTAINERS" ]; then
    NEEDED_CONTAINERS=$MIN_CONTAINERS
  fi

  if [ "$NEEDED_CONTAINERS" -gt "$MAX_CONTAINERS" ]; then
    NEEDED_CONTAINERS=$MAX_CONTAINERS
  fi

  CURRENT=$(docker compose ps $SERVICE_NAME --format "{{.State}}" 2>/dev/null | grep -c "running" || echo "1")

  if [ "$NEEDED_CONTAINERS" -ne "$CURRENT" ]; then
    echo "Масштабируем $SERVICE_NAME: $CURRENT -> $NEEDED_CONTAINERS"
    docker compose up -d --scale "$SERVICE_NAME"="$NEEDED_CONTAINERS"
  else
    echo "Количество контейнеров не изменилось ($CURRENT)"
  fi

  sleep 15

done
