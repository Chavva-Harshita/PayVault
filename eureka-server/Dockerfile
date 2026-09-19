# Build context is the repo root (see docker-compose.yml's build.context),
# not this directory - needed because this module's pom.xml has a
# <parent> reference to ../pom.xml, and `mvn -pl eureka-server -am` needs
# that parent visible to resolve dependency versions.

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# A production-grade Dockerfile would copy only the pom.xml files first,
# run `mvn dependency:go-offline`, then copy sources - so Docker's layer
# cache survives source-only changes without re-downloading the internet.
# Skipped here for simplicity, per this project's "don't overcomplicate"
# guidance - every code change re-runs a full Maven build.
COPY . .
RUN mvn -pl eureka-server -am package -DskipTests -q

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /workspace/eureka-server/target/eureka-server-1.0.0.jar app.jar

EXPOSE 8761
ENTRYPOINT ["java", "-jar", "app.jar"]
