# ---------- Stage 1: build ----------
FROM maven:3.9.6-eclipse-temurin-21-alpine AS builder
WORKDIR /app

# Copy only pom.xml first so the dependency layer is cached until pom.xml changes
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: run ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Do not run the application as root
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
