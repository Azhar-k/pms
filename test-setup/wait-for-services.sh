#!/bin/bash

# Helper script to wait for Docker services to be ready
# Usage: ./wait-for-services.sh
# Note: This script should be run from the project root directory

set -e

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the project root directory (parent of docker directory)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Change to project root to ensure relative paths work correctly
cd "$PROJECT_ROOT"

echo "Waiting for PostgreSQL to be ready..."
timeout=60
elapsed=0
while ! docker compose -f test-setup/docker-compose.yml exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    if [ $elapsed -ge $timeout ]; then
        echo "ERROR: PostgreSQL failed to start within ${timeout} seconds"
        docker compose -f test-setup/docker-compose.yml logs postgres
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
        docker compose -f test-setup/docker-compose.yml logs app
        exit 1
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    echo -n "."
done
echo " Application is ready!"

echo "All services are ready!"

