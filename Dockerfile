## Build Stage ##
FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean install -DskipTests

## Run Stage ##
FROM openjdk:17-jdk-alpine
WORKDIR /run
COPY --from=build /app/target/ecommerce-1.0.0.jar /run/ecommerce-1.0.0.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/run/ecommerce-1.0.0.jar"]
