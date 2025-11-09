#!/bin/bash

# Helper script to wait for Docker services to be ready
# Usage: ./wait-for-services.sh

set -e

echo "Waiting for PostgreSQL to be ready..."
timeout=60
elapsed=0
while ! docker compose exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    if [ $elapsed -ge $timeout ]; then
        echo "ERROR: PostgreSQL failed to start within ${timeout} seconds"
        docker compose logs postgres
        exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
    echo -n "."
done
echo " PostgreSQL is ready!"

echo "Waiting for application to be ready..."
timeout=120
elapsed=0
while ! curl -f http://localhost:8081/api/guests > /dev/null 2>&1; do
    if [ $elapsed -ge $timeout ]; then
        echo "ERROR: Application failed to start within ${timeout} seconds"
        docker compose logs app
        exit 1
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    echo -n "."
done
echo " Application is ready!"

echo "All services are ready!"

