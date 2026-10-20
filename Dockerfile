FROM bellsoft/liberica-openjdk-alpine:25
WORKDIR /app
COPY target/pagos-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8085
ENTRYPOINT ["java", "-jar", "app.jar"]
