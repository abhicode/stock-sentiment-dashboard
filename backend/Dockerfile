# Step 1: Use Maven image to build the JAR
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cached for faster builds)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests

# Step 2: Use lightweight JDK to run the JAR
FROM eclipse-temurin:21-jdk
WORKDIR /app

# Copy built JAR from builder
COPY --from=build /app/target/*.jar app.jar

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]

# Expose Spring Boot default port
EXPOSE 8080
