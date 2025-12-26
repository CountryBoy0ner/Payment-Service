
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -Dmaven.test.skip=true -DskipTests dependency:resolve -DincludeScope=runtime \
 && ./mvnw -q -Dmaven.test.skip=true -DskipTests dependency:resolve-plugins

COPY src src
RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -Dmaven.test.skip=true package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085
ENTRYPOINT ["java","-jar","/app/app.jar"]
