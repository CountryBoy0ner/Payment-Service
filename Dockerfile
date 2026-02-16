FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests dependency:resolve -DincludeScope=runtime \
 && ./mvnw -q -DskipTests dependency:resolve-plugins

COPY src src

RUN --mount=type=cache,target=/root/.m2 \
    ./mvnw -q -DskipTests package

RUN mkdir -p /app/extracted \
 && cd /app/extracted \
 && jar -xf /app/target/*.jar


FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/extracted/BOOT-INF/lib /app/lib
COPY --from=build /app/extracted/BOOT-INF/classes /app/classes
COPY --from=build /app/extracted/META-INF /app/META-INF

EXPOSE 8085

ENTRYPOINT ["java","-cp","/app/classes:/app/lib/*","com.innowise.PaymentServiceApplication"]
