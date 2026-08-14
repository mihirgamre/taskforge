# syntax=docker/dockerfile:1
ARG MODULE

FROM eclipse-temurin:25-jdk AS build
WORKDIR /workspace
COPY . .
ARG MODULE
RUN sh ./mvnw -pl ${MODULE} -am package -DskipTests

FROM eclipse-temurin:25-jre
ARG MODULE
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
WORKDIR /app
RUN addgroup --system taskforge && adduser --system --ingroup taskforge taskforge
COPY --from=build /workspace/${MODULE}/target/${MODULE}-0.1.0-SNAPSHOT.jar /app/app.jar
USER taskforge
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
