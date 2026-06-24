FROM maven:3.9.16-eclipse-temurin-21-alpine AS build
WORKDIR /app

#COPY ./scripts/fetch-key.sh ./fetch-key.sh

COPY ./services/pom.xml ./pom.xml

COPY ./services/common/pom.xml ./common/pom.xml
COPY ./services/key-management/pom.xml ./key-management/pom.xml

COPY ./services/common/src ./common/src
COPY ./services/key-management/src ./key-management/src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -P key-management -pl key-management -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/key-management/target/key-management-*.jar app.jar
#COPY --from=build /app/fetch-key.sh ./fetch-key.sh
#
#RUN apk add --no-cache jq
#RUN chmod +x ./fetch-key.sh

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --retries=3 --start-period=40s \
    CMD wget -qO- http://localhost:8080/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
