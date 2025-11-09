# Docker-Based Integration Testing

This project includes Docker-based integration testing setup that automatically builds and runs the application and database in Docker containers.

## Overview

The Docker setup includes:
- **PostgreSQL Database**: Running on port **5433** (mapped from container port 5432)
- **Spring Boot Application**: Running on port **8081** (mapped from container port 8080)

## Prerequisites

1. **Docker** and **Docker Compose** installed
   - Docker: https://docs.docker.com/get-docker/
   - Docker Compose: Usually included with Docker Desktop

2. Verify Docker is running:
   ```bash
   docker --version
   docker compose version
   ```

## Quick Start

### Option 1: Using the Test Script (Recommended)

**Linux/Mac:**
```bash
chmod +x docker/docker-test.sh
./docker/docker-test.sh
```

**Windows:**
```cmd
docker\docker-test.bat
```

**Skip Docker Build (use existing images):**
```bash
# Linux/Mac
./docker/docker-test.sh --skip-build
# or
./docker/docker-test.sh -s
# or via environment variable
SKIP_DOCKER_BUILD=true ./docker/docker-test.sh

# Windows
docker\docker-test.bat --skip-build
# or
docker\docker-test.bat -s
# or via environment variable
set SKIP_DOCKER_BUILD=true
docker\docker-test.bat
```

The script will:
1. Build Docker images (unless `--skip-build` is used)
2. Start containers (PostgreSQL + Application)
3. Wait for services to be healthy
4. Run integration tests
5. Clean up containers automatically

### Option 2: Manual Docker Management

**1. Build and start containers:**
```bash
# Build and start (rebuilds images)
docker compose -f docker/docker-compose.yml up -d --build

# Or start without rebuilding (uses existing images)
docker compose -f docker/docker-compose.yml up -d
```

**2. Wait for services to be ready:**
```bash
# Check PostgreSQL
docker compose -f docker/docker-compose.yml exec postgres pg_isready -U postgres

# Check Application (wait until it responds)
curl http://localhost:8081/api/guests
```

**3. Run tests:**
```bash
mvn clean test -Dtest=GuestControllerIntegrationTest
```

**4. Stop and cleanup:**
```bash
docker compose -f docker/docker-compose.yml down -v
```

## Port Configuration

The containers use different ports to avoid conflicts:

| Service | Container Port | Host Port |
|---------|---------------|-----------|
| PostgreSQL | 5432 | **5433** |
| Application | 8080 | **8081** |

You can change these ports by modifying `docker/docker-compose.yml`:
```yaml
services:
  postgres:
    ports:
      - "YOUR_PORT:5432"  # Change YOUR_PORT
  
  app:
    ports:
      - "YOUR_PORT:8080"  # Change YOUR_PORT
```

Then update `TestConfig.java` or set environment variables:
```bash
export TEST_API_PORT=YOUR_PORT
mvn test
```

## Test Configuration

The test configuration automatically uses:
- **API Host**: `localhost` (configurable via `TEST_API_HOST` env var)
- **API Port**: `8081` (configurable via `TEST_API_PORT` env var)

You can override these via:
- Environment variables: `TEST_API_HOST`, `TEST_API_PORT`
- System properties: `-Dtest.api.host=localhost -Dtest.api.port=8081`
- Maven profile: `mvn test -Pdocker-test`

## Docker Compose Services

### PostgreSQL Service
- **Image**: `postgres:15-alpine`
- **Database**: `pms`
- **User**: `postgres`
- **Password**: `postgres`
- **Health Check**: `pg_isready`

### Application Service
- **Build**: Uses `docker/Dockerfile` (multi-stage build)
- **Depends on**: PostgreSQL (waits for health check)
- **Health Check**: HTTP endpoint check
- **Environment**: Configured via `docker/docker-compose.yml`

## Troubleshooting

### Containers won't start
```bash
# Check logs
docker compose -f docker/docker-compose.yml logs

# Check specific service
docker compose -f docker/docker-compose.yml logs app
docker compose -f docker/docker-compose.yml logs postgres
```

### Port already in use
If ports 5433 or 8081 are already in use:
1. Change ports in `docker/docker-compose.yml`
2. Update `TestConfig.java` or set environment variables
3. Or stop the conflicting service

### Application fails to connect to database
- Ensure PostgreSQL container is healthy: `docker compose -f docker/docker-compose.yml ps`
- Check database connection string in `docker/docker-compose.yml`
- Verify network connectivity: `docker compose -f docker/docker-compose.yml exec app ping postgres`

### Tests timeout waiting for application
- Increase timeout in test script
- Check application logs: `docker compose -f docker/docker-compose.yml logs app --tail=100`
- Verify application health: `curl http://localhost:8081/api/guests`

### Clean up everything
```bash
# Stop and remove containers, networks, and volumes
docker compose -f docker/docker-compose.yml down -v

# Remove images (optional)
docker compose -f docker/docker-compose.yml down --rmi all

# Remove all unused Docker resources (be careful!)
docker system prune -a
```

## Dockerfile Details

The `docker/Dockerfile` uses a multi-stage build:
1. **Build stage**: Uses Maven to compile and package the application
2. **Runtime stage**: Uses JRE-only image (smaller footprint)

This results in a smaller final image (~200MB vs ~800MB).

## Continuous Integration

For CI/CD pipelines, you can use:

```yaml
# Example GitHub Actions
- name: Run Docker tests
  run: |
    docker compose -f docker/docker-compose.yml up -d --build
    ./docker/wait-for-services.sh  # Custom script to wait
    mvn test
    docker compose -f docker/docker-compose.yml down -v
```

## Manual Container Management

**View running containers:**
```bash
docker compose -f docker/docker-compose.yml ps
```

**View logs:**
```bash
docker compose -f docker/docker-compose.yml logs -f app
docker compose -f docker/docker-compose.yml logs -f postgres
```

**Execute commands in container:**
```bash
docker compose -f docker/docker-compose.yml exec app sh
docker compose -f docker/docker-compose.yml exec postgres psql -U postgres -d pms
```

**Restart services:**
```bash
docker compose -f docker/docker-compose.yml restart
```

**Rebuild and restart:**
```bash
docker compose -f docker/docker-compose.yml up -d --build
```

## Environment Variables

You can override configuration via environment variables:

```bash
export TEST_API_HOST=localhost
export TEST_API_PORT=8081
mvn test
```

Or in `docker/docker-compose.yml` for the application:
```yaml
environment:
  SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/pms
  # ... other vars
```

## Notes

- The Docker setup is **only for testing** - not for production
- Test data is automatically cleaned up after tests complete
- Containers are automatically stopped and removed after test script completes
- Database data persists in a Docker volume (removed with `-v` flag)

