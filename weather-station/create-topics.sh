#!/usr/bin/env bash
# create-topics.sh
# Run this once after Kafka starts to create the required topics.
# Usage: ./create-topics.sh
# Make executable first: chmod +x create-topics.sh

set -e

echo "Waiting for Kafka to be ready..."
sleep 5

# The "weather" topic receives messages from all 10 stations.
# 10 partitions = 1 partition per station (Kafka routes by key hash).
# This ensures messages from the same station arrive in order.
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic weather \
  --partitions 10 \
  --replication-factor 1

echo "Created topic: weather (10 partitions)"

# The "rain-alerts" topic receives rain alert messages from the detector.
# 1 partition is enough — alerts are low-volume.
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --create --if-not-exists \
  --topic rain-alerts \
  --partitions 1 \
  --replication-factor 1

echo "Created topic: rain-alerts (1 partition)"

# Show all topics to confirm
echo ""
echo "All topics:"
docker compose exec kafka kafka-topics \
  --bootstrap-server localhost:9092 --list
