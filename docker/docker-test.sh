#!/bin/bash

# Script to build Docker containers, run tests, and cleanup
# Usage: ./docker-test.sh [--skip-build]
#   --skip-build: Skip Docker image build phase (use existing images)
# 
# Note: This script should be run from the project root directory

set -e  # Exit on error

# Get the directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# Get the project root directory (parent of docker directory)
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

# Change to project root to ensure relative paths work correctly
cd "$PROJECT_ROOT"

# Parse command line arguments
SKIP_BUILD=false
if [[ "$1" == "--skip-build" ]] || [[ "$1" == "-s" ]]; then
    SKIP_BUILD=true
fi

# Also check environment variable
if [[ "${SKIP_DOCKER_BUILD}" == "true" ]] || [[ "${SKIP_DOCKER_BUILD}" == "1" ]]; then
    SKIP_BUILD=true
fi

echo "=========================================="
echo "Docker Integration Test Runner"
echo "=========================================="

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Function to cleanup
cleanup() {
    echo -e "\n${YELLOW}Cleaning up Docker containers...${NC}"
    docker compose -f docker/docker-compose.yml down -v
    echo -e "${GREEN}Cleanup completed${NC}"
}

# Trap to ensure cleanup on script exit
trap cleanup EXIT

# Build Docker images (unless skipped)
if [ "$SKIP_BUILD" = true ]; then
    echo -e "${YELLOW}Step 1: Skipping Docker image build (using existing images)...${NC}"
else
    echo -e "${YELLOW}Step 1: Building Docker images...${NC}"
    docker compose -f docker/docker-compose.yml build
fi

echo -e "\n${YELLOW}Step 2: Starting Docker containers...${NC}"
docker compose -f docker/docker-compose.yml up -d

echo -e "\n${YELLOW}Step 3: Waiting for services to be healthy...${NC}"
echo "Waiting for PostgreSQL..."
timeout=60
elapsed=0
while ! docker compose -f docker/docker-compose.yml exec -T postgres pg_isready -U postgres > /dev/null 2>&1; do
    if [ $elapsed -ge $timeout ]; then
        echo -e "${RED}PostgreSQL failed to start within ${timeout} seconds${NC}"
        docker compose -f docker/docker-compose.yml logs postgres
        exit 1
    fi
    sleep 2
    elapsed=$((elapsed + 2))
    echo -n "."
done
echo -e " ${GREEN}PostgreSQL is ready${NC}"

echo "Waiting for application..."
timeout=180
elapsed=0
max_attempts=$((timeout / 3))
attempt=0
# Use health check endpoint which doesn't require authentication
while ! curl -f -s http://localhost:8081/health > /dev/null 2>&1; do
    attempt=$((attempt + 1))
    if [ $elapsed -ge $timeout ]; then
        echo -e "\n${RED}Application failed to start within ${timeout} seconds${NC}"
        echo -e "${YELLOW}Application logs:${NC}"
        docker compose -f docker/docker-compose.yml logs app --tail=50
        exit 1
    fi
    sleep 3
    elapsed=$((elapsed + 3))
    if [ $((attempt % 5)) -eq 0 ]; then
        echo -n " [${elapsed}s]"
    else
        echo -n "."
    fi
done
echo -e " ${GREEN}Application is ready (took ${elapsed}s)${NC}"

echo -e "\n${YELLOW}Step 4: Running integration tests...${NC}"
mvn clean test

TEST_RESULT=$?

if [ $TEST_RESULT -eq 0 ]; then
    echo -e "\n${GREEN}=========================================="
    echo "All tests passed!"
    echo "==========================================${NC}"
else
    echo -e "\n${RED}=========================================="
    echo "Tests failed!"
    echo "==========================================${NC}"
fi

# Show container logs if tests failed
if [ $TEST_RESULT -ne 0 ]; then
    echo -e "\n${YELLOW}Application logs:${NC}"
    docker compose -f docker/docker-compose.yml logs app --tail=50
fi

exit $TEST_RESULT

