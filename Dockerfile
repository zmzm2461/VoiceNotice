# ===== 1단계: build =====
FROM gradle:8.10.1-jdk21 AS build
WORKDIR /app

COPY . .

RUN gradle --no-daemon clean build -x test --stacktrace --info

# ===== 2단계: run =====
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]