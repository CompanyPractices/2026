FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY mvnw ./
COPY .mvn ./.mvn

COPY pom.xml ./
COPY src ./src
RUN ./mvnw package -DskipTests -q

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
ARG PORT=8083
ENV PORT=${PORT}
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "app.jar"]
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:${PORT}/health || exit 1
