# Build context is the repo root - see eureka-server/Dockerfile's comment
# for why.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace
COPY . .
RUN mvn -pl user-service -am package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service-1.0.0.jar app.jar

EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
