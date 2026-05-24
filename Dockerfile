## Build Stage ##
FROM maven:3.8.3-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean install

## Run Stage ##
FROM eclipse-temurin:17.0.8.1_1-jre-ubi9-minimal
WORKDIR /run
COPY --from=build /app/target/ecommerce-1.0.0.jar /run/ecommerce-1.0.0.jar
EXPOSE 8888
ENV JAVA_OPTIONS="-Xmx2048m -Xms256m"
ENTRYPOINT java -jar $JAVA_OPTIONS /run/ecommerce-1.0.0.jar
