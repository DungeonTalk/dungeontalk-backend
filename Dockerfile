# ---------- build ----------
FROM gradle:8.14.3-jdk21-alpine AS builder
WORKDIR /app

# Wrapper 및 스크립트가 컨텍스트에 반드시 존재해야 함 (위 0)단계 확인!)
COPY gradlew ./gradlew
COPY gradle/wrapper/ ./gradle/wrapper/
RUN chmod +x gradlew

# 빌드 스크립트만 먼저 복사해서 의존성 캐시 최적화
COPY settings.gradle settings.gradle.kts* build.gradle build.gradle.kts* gradle.properties* ./
RUN ./gradlew --no-daemon dependencies || true

# 소스 마지막에 복사 → 캐시 효율
COPY src ./src

# 테스트 스킵 빌드
RUN ./gradlew --no-daemon clean bootJar -x test

# ---------- run ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 비루트 사용자(선택)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

COPY --from=builder /app/build/libs/*.jar /app/app.jar

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC"
EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
