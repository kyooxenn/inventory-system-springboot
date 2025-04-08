FROM eclipse-temurin:17-jdk-alpine
ENV APP_HOME=/app
WORKDIR $APP_HOME
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
