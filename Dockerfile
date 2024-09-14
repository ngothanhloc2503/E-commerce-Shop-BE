FROM maven:3.8.6-openjdk-17 AS build
WORKDIR /app
COPY . .
RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim
WORKDIR /app
COPY --from=build /app/target/ecommerce-1.0.0.jar.jar ecommerce-1.0.0.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "ecommerce-1.0.0.jar"]
