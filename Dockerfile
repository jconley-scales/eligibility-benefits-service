# syntax=docker/dockerfile:1

# Build stage: compile and package on a full JDK. Tests run in CI, not here,
# because they need Docker for Testcontainers.
FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY .mvn/ .mvn/
COPY --chmod=755 mvnw ./
COPY pom.xml ./
COPY src/ src/
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw --batch-mode --no-transfer-progress package -DskipTests \
 && cp target/eligibility-benefits-service-*.jar application.jar \
 && java -Djarmode=tools -jar application.jar extract --layers --destination extracted

# Runtime stage: a JRE only, running as a non-root user. Dependencies and
# application classes are separate layers, so a code change rebuilds only the last one.
FROM eclipse-temurin:25-jre
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
USER app
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "application.jar"]
