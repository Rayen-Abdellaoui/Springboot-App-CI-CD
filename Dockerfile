FROM adoptopenjdk/openjdk11:alpine-jre

ARG artifact=Medsirat_Internship.jar

WORKDIR /opt/app

COPY ${artifact} app.jar

ENTRYPOINT ["java","-jar","app.jar"]
