FROM openjdk:11
WORKDIR /usr/src/app

COPY . .
ENTRYPOINT ["java", "-Dspring.profiles.active=dev", "-jar", "app.jar"]