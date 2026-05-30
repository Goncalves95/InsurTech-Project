# ─── Stage 1: Build ──────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /build

# Resolve dependencies before copying source — maximises Docker layer cache hits
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B --no-transfer-progress

COPY src/ ./src/
RUN ./mvnw package -DskipTests -B --no-transfer-progress

# ─── Stage 2: Runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S insurtech && adduser -S insurtech -G insurtech

WORKDIR /app
COPY --from=builder /build/target/*.jar app.jar

EXPOSE 8080
USER insurtech:insurtech

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-jar", "app.jar"]
