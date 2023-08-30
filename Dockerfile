FROM gradle:8.2-jdk17 AS build
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle build -Dorg.gradle.daemon=false

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /work/build/libs/*.jar ./app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","./app.jar"]
