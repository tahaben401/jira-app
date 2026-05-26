FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

ARG MODULE

COPY . .

RUN mvn -pl "${MODULE}" -am -Dmaven.test.skip=true package \
    && JAR_FILE="$(find "${MODULE}/target" -maxdepth 1 -name '*.jar' ! -name '*.original' | head -n 1)" \
    && cp "${JAR_FILE}" /tmp/app.jar

FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY --from=build /tmp/app.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
