# GitHub Actions Workflow Migration Guide for Spring Boot Maven

This document clearly explains all the changes required to enable the Docker Build and Push workflow (to GitHub Container Registry - GHCR) in your other Spring Boot Maven repository. You can give this entire document to an LLM to make the necessary changes in that repository.

## 1. Create a Multi-Stage Dockerfile
Your project needs a `Dockerfile` at its root directory to define how the application is built and run. This multi-stage setup builds the JAR with Maven in the first stage and packages it in a minimal JRE for the runtime stage to keep the final image small.

Create a file named `Dockerfile` in the root of the repository with the following content:

```dockerfile
# Stage 1: The Build Stage (Java 17 SDK for compilation)
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the Maven project files first to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Compile, test, and package the application into a JAR
RUN mvn clean package -DskipTests -B

# Stage 2: The Runtime Stage (Minimal Java 17 JRE for running)
FROM eclipse-temurin:17-jre
WORKDIR /app

# Install wget for healthcheck (as root, before switching to non-root user)
RUN apt-get update && apt-get install -y --no-install-recommends wget && \
    rm -rf /var/lib/apt/lists/*

# Create a non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

# Copy the final executable JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Expose the application port
EXPOSE 8080

# Add healthcheck (using the custom /health endpoint or Spring Boot actuator endpoint)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 2. Add an Optional `.dockerignore` (Recommended)
To prevent unnecessary files from being copied into the Docker build context (which speeds up the build process), create a `.dockerignore` file in the root directory:

```text
.git
.gitignore
.github
target/
*.md
Dockerfile
```

## 3. Create the GitHub Actions Workflow File
You need to add a YAML configuration file that interacts with GitHub Actions to build and push the Docker image. 

Create a file at `.github/workflows/docker-build-push.yml` with the following content:

```yaml
name: Build and Push Docker Image

on:
  push:
    branches:
      - main
      - master
    tags:
      - 'v*'
  pull_request:
    branches:
      - main
      - master

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  build-and-push:
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write

    steps:
      - name: Checkout repository
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to GitHub Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata (tags, labels) for Docker
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=semver,pattern={{major}}
            type=sha,prefix=sha-
            type=raw,value=latest,enable={{is_default_branch}}

      - name: Build and push Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          file: ./Dockerfile
          push: ${{ github.event_name != 'pull_request' }}
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max
          # Enables building for multiple architectures
          platforms: linux/amd64,linux/arm64
```

## LLM Verification Checklist:
When applying these changes, ensure you process the following checklist:
1. **Java Version Matching**: If the other project uses a different version of Java (e.g. Java 21), ensure you update the JDK versions in both `docker-build-push.yml` (`java-version: '21'`) and `Dockerfile` (`maven:3.9.5-eclipse-temurin-21`, `eclipse-temurin:21-jre`).
2. **Healthcheck Endpoint**: Validate whether the application has a `/health` endpoint defined. If it uses Spring Boot Actuator, the healthcheck URL might need to be changed to `http://localhost:8080/actuator/health`.
3. **Repository Settings Override**: Before merging this action, remind the user to configure their GitHub Repository (`Settings > Actions > General > Workflow permissions`) to **Read and write permissions** so the Action has rights to push to GHCR.
