# 1. Build Stage
FROM gradle:jdk17 as builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradlew gradlew.bat ./ 
COPY gradle ./gradle
COPY src ./src

RUN ./gradlew build -x test

# 2. Runtime Stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

COPY --from=builder /app/build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
