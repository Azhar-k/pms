# Stage 1: The Build Stage (Java 17 SDK for compilation)
FROM maven:3.9.5-eclipse-temurin-17 AS build
WORKDIR /app

# Copy the Maven project files first to leverage Docker cache
COPY pom.xml .
COPY src ./src

# Compile, test, and package the application into a JAR
# The '-B' is for non-interactive (batch) mode
RUN mvn clean package -DskipTests

# Stage 2: The Runtime Stage (Minimal Java 17 JRE for running)
# Use a JRE-only image for a smaller, more secure final image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Copy the final executable JAR from the build stage
COPY --from=build /app/target/*.jar app.jar

# Command to run the application when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]