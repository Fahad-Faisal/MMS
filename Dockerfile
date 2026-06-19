




# Multi-stage build for optimized image size
# Stage 1: Build the application
FROM maven:3.9.9-eclipse-temurin-21 AS build

# Set working directory
WORKDIR /app

# Copy pom.xml and download dependencies (cached layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build the application
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:21-jre-alpine

# Create a non-root user to run the application
RUN addgroup -S mms && adduser -S mms -G mms

# Set working directory
WORKDIR /app

# Install curl for health checks (optional)
RUN apk add --no-cache curl

# Copy the JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# Change ownership to non-root user
RUN chown -R mms:mms /app

# Switch to non-root user
USER mms

# Expose the application port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/api/health || exit 1

# Run the application with default configuration
# Override configuration using environment variables at runtime
ENTRYPOINT ["java", "-jar", "app.jar"]