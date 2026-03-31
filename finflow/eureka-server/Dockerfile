FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY eureka-server/target/*.jar app.jar
ENTRYPOINT ["java","-jar","app.jar"]
