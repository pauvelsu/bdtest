FROM eclipse-temurin:17-jdk-alpine

WORKDIR /app

COPY target/microbenchmarks-1.0.0-jar-with-dependencies.jar app.jar

CMD ["java", "-jar", "app.jar"]
