FROM public.ecr.aws/docker/library/maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /build
COPY pom.xml .
COPY switch/pom.xml switch/
COPY switch/src switch/src
COPY merchant-acquirer/pom.xml merchant-acquirer/
COPY terminal-simulator/pom.xml terminal-simulator/
RUN mvn -pl switch package -DskipTests -q

FROM public.ecr.aws/docker/library/eclipse-temurin:21-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /build/switch/target/*.jar app.jar
ENV PORT=8082
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
HEALTHCHECK --interval=10s --timeout=3s --retries=3 \
    CMD wget -qO- http://localhost:8082/health || exit 1
