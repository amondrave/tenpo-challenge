FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

COPY gradlew gradlew.bat ./
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
RUN ./gradlew --no-daemon help

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:21-jre-alpine AS runtime
RUN addgroup -S tenpo && adduser -S tenpo -G tenpo
WORKDIR /app
COPY --from=build /workspace/build/libs/*.jar app.jar
USER tenpo

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
