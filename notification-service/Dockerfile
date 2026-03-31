FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY notification-service/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
