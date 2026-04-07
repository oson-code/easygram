# ── Stage 1: build ─────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jdk-alpine AS build

WORKDIR /workspace

# Cache dependency downloads before copying source
COPY pom.xml .
RUN apk add --no-cache maven && \
    mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: runtime ────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine

# Non-root user for security
RUN addgroup -S bot && adduser -S bot -G bot

WORKDIR /app

COPY --from=build /workspace/target/*.jar app.jar

RUN chown bot:bot app.jar

USER bot

# Webhook port (only needed when easygram.update.transport=WEBHOOK)
EXPOSE 8080

ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
