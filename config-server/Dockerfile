FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY config-server/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
