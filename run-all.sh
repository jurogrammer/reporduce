#!/bin/bash
set -e

LOG_DIR=logs
mkdir -p $LOG_DIR

trap 'echo "[🧹] Cleaning up..."; kill $SERVICE_PID $DOMAIN_PID 2>/dev/null || true; exit' INT TERM

echo "[🚀] Starting domain-server..."
./gradlew :domain-server:bootRun > $LOG_DIR/domain-server.log 2>&1 &
DOMAIN_PID=$!

echo "[🚀] Starting service-server..."
./gradlew :service-server:bootRun > $LOG_DIR/service-server.log 2>&1 &
SERVICE_PID=$!

echo "[⏳] Waiting for servers to start..."
sleep 5

echo "[🎯] Running client..."
./gradlew :client:bootRun > $LOG_DIR/client.log 2>&1

echo "[🛑] Shutting down background servers..."
kill $SERVICE_PID $DOMAIN_PID 2>/dev/null || true
