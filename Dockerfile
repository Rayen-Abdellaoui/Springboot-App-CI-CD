FROM eclipse-temurin:11-jre-alpine

ARG artifact=target/spring-boot-web.jar
WORKDIR /opt/app

COPY ${artifact} app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
