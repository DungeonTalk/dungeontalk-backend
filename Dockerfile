# ---------- build ----------
FROM amazoncorretto:21 AS builder
WORKDIR /app

COPY gradlew gradlew.bat gradle/ ./
RUN chmod +x gradlew
COPY settings.gradle settings.gradle.kts* build.gradle build.gradle.kts* gradle.properties* ./
RUN ./gradlew --no-daemon dependencies || true

COPY src ./src
RUN ./gradlew --no-daemon -x test bootJar

# ---------- run ----------
FROM amazoncorretto:21-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar

ENV JAVA_OPTS=""
# 프로필은 Dockerfile에서 고정하지 않고 Railway 변수로 지정
# ARG APP_PROFILE=prod
# ENV SPRING_PROFILES_ACTIVE=${APP_PROFILE}

EXPOSE 8080
ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar /app/app.jar"]
