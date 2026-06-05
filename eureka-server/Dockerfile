FROM eclipse-temurin:21-jre-alpine

COPY target/eureka-server.jar /app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar","/app.jar"]