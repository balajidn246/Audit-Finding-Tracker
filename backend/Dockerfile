FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package

FROM eclipse-temurin:25-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S verityops \
    && adduser -S -G verityops -h /app verityops
WORKDIR /app
COPY --from=build --chown=verityops:verityops /workspace/target/audit-finding-tracker-1.0.0.jar /app/application.jar
RUN mkdir -p /var/verityops/evidence && chown -R verityops:verityops /var/verityops /app
USER verityops
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-XX:+ExitOnOutOfMemoryError", "-jar", "/app/application.jar"]
