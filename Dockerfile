FROM eclipse-temurin:21-jre

WORKDIR /app

COPY app/build/libs/*.jar app.jar

EXPOSE 9898

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
