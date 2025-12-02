# Stage 1: The Build Stage (Java 17 SDK for compilation)
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the Maven project files first to leverage Docker cache
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Compile, test, and package the application into a JAR
# The '-B' is for non-interactive (batch) mode
RUN mvn clean package -DskipTests -B

# Stage 2: The Runtime Stage (Minimal Java 17 JRE for running)
# Use a JRE-only image that supports both AMD64 and ARM64 platforms
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

# Add healthcheck (using the custom /health endpoint)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/health || exit 1

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]