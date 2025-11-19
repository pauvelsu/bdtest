FROM eclipse-temurin:21-jdk
WORKDIR /app

COPY target/control-module-1.0.0-jar-with-dependencies.jar app.jar

EXPOSE 7000
ENTRYPOINT ["java", "-jar", "app.jar"]
