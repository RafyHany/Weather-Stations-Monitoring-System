#!/bin/bash

# Exit immediately if a command exits with a non-zero status
set -e

echo "==========================================="
echo "🚀 STEP 1: Starting Minikube Cluster"
echo "==========================================="
minikube start  --driver=docker --cpus=4 --memory=6144

echo ""
echo "==========================================="
echo "🔌 STEP 2: Pointing terminal to Minikube Docker"
echo "==========================================="
eval $(minikube docker-env)

echo ""
echo "==========================================="
echo "🏗️ STEP 3: Building Custom Java Images Local Cache"
echo "==========================================="

echo "Building Weather Stations Mock."
docker build -t weather-station:1.0 -f ./weather-station/Dockerfile.station ./weather-station


echo "Building Rain Detector..."
docker build -t rain-detector:1.0 -f ./weather-station/Dockerfile.detector ./weather-station

#
echo "Building Central Station..."
docker build -t central-station:1.0 -f ./Central_Station/Dockerfile ./Central_Station

echo "Building Search Integration..."
docker build -t search-integration:1.0 -f ./demo/Dockerfile ./demo

echo ""
echo "==========================================="
echo "🗂️ STEP 4: Creating Single Namespace"
echo "==========================================="
# Applying your namespace file inside the k8s folder
kubectl apply -f ./k8s/namespace.yaml

echo ""
echo "==========================================="
echo "📄 STEP 5: Applying Kubernetes Infrastructure Manifests"
echo "==========================================="

echo "Applying Shared Storage..."
kubectl apply -f ./k8s/shared_storage.yaml

echo "Applying Broker Infrastructure (Zookeeper & Kafka)..."
kubectl apply -f ./k8s/broker/zookeeper.yaml
kubectl apply -f ./k8s/broker/kafka.yaml

echo "Applying Search Infrastructure (Elasticsearch & Kibana)..."
kubectl apply -f ./k8s/search/elastic_search.yaml
kubectl apply -f ./k8s/search/kibana.yaml
kubectl apply -f ./k8s/search/search_integration.yaml

echo ""
echo "==========================================="
echo "⏳ STEP 6: Waiting for Infrastructure Components to Boot"
echo "==========================================="
echo "Waiting for Kafka & Elasticsearch to be ready..."
kubectl wait --namespace weather-station --for=condition=ready pod -l app=kafka --timeout=300s
kubectl wait --namespace weather-station --for=condition=ready pod -l app=elasticsearch --timeout=600s

echo ""
echo "==========================================="
echo "🚀 STEP 7: Applying Core Weather Services"
echo "==========================================="
echo "Applying Core Weather Services (Central Station, Rain Detector, Weather Station)..."
kubectl apply -f ./k8s/weather_services/central_station.yaml
kubectl apply -f ./k8s/weather_services/rain_detector.yaml
kubectl apply -f ./k8s/weather_services/weather_station.yaml

echo "Waiting for Weather Services to be ready..."
kubectl wait --namespace weather-station --for=condition=ready pod -l app=central-station --timeout=300s
kubectl wait --namespace weather-station --for=condition=ready pod -l app=rain-detector --timeout=300s
kubectl wait --namespace weather-station --for=condition=ready pod -l app=weather-station --timeout=300s

echo ""
echo "==========================================="
echo "🌐 STEP 8: Launching Tunnels and Port Forwards"
echo "==========================================="
echo "Opening Kafka External Pipeline (Port 30092)..."
kubectl port-forward svc/kafka 30092:9092 -n weather-station &
kubectl port-forward svc/central-station 30080:8080 -n weather-station &

echo "Opening Kibana Dashboard Tunnel..."
minikube service kibana -n weather-station

echo "==========================================="
echo "🎉 Cluster Architecture successfully deployed!"
echo "==========================================="