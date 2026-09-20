# Build context is the repo root - see eureka-server/Dockerfile's comment
# for why.
#
# One image, run as two containers in docker-compose.yml (wallet-service
# and wallet-service-2) with different WALLET_SERVICE_PORT / active
# profile - this is the same "same codebase, different port" idea from
# Phase 8, just containerized.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl wallet-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/wallet-service/target/wallet-service-1.0.0.jar app.jar

EXPOSE 8083 8093
ENTRYPOINT ["java", "-jar", "app.jar"]
