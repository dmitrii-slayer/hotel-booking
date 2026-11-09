FROM maven:3.9.9-eclipse-temurin-21-alpine AS builder

WORKDIR /build

COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/
COPY api-gateway/pom.xml api-gateway/
COPY hotel-service/pom.xml hotel-service/
COPY booking-service/pom.xml booking-service/

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl booking-service dependency:go-offline

COPY booking-service/src booking-service/src

RUN --mount=type=cache,target=/root/.m2 \
    mvn -B -ntp -pl booking-service -DskipTests package

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -Dfile.encoding=UTF-8"

COPY --from=builder /build/booking-service/target/booking-service-*.jar app.jar

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
