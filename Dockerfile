FROM openjdk:17-jdk-slim AS build
COPY . /work
WORKDIR /work
RUN ./gradlew build -Dorg.gradle.daemon=false

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /work/build/libs/*.jar ./app.jar
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","./app.jar"]
