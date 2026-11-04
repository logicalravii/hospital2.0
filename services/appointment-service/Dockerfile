# Stage 1: Build
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

# Copy Maven wrapper and POM hierarchy
COPY mvnw pom.xml ./
COPY .mvn/ .mvn/
COPY common/pom.xml common/
COPY services/identity-service/pom.xml services/identity-service/
COPY services/patient-service/pom.xml services/patient-service/
COPY services/doctor-service/pom.xml services/doctor-service/
COPY services/appointment-service/pom.xml services/appointment-service/
COPY services/consultation-service/pom.xml services/consultation-service/
COPY services/api-gateway/pom.xml services/api-gateway/

# Copy common library sources and build
COPY common/src common/src
RUN chmod +x mvnw && ./mvnw install -pl common -DskipTests

# Copy service sources and package
COPY services/appointment-service/src services/appointment-service/src
RUN ./mvnw package -pl services/appointment-service -am -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S hms && adduser -S hms -G hms
USER hms:hms

COPY --from=builder /workspace/services/appointment-service/target/appointment-service-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]

