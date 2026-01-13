# Stage 1: Build with Maven and JDK 21 image
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .

# if you change Java code but not pom.xml, Docker reuses this layer and doesn't download everything again
RUN mvn dependency:go-offline


# Copy the code and build the JAR
COPY src ./src
RUN mvn clean package -DskipTests


# Stage 2: Runtime
# Uses a lighter image with only JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# Copy only the JAR generated in the previous stage
COPY --from=build /app/target/*.jar app.jar


# Expose the default Spring Boot port
EXPOSE 8080


# Memory settings optimized for small containers
# -XX:+UseContainerSupport: JVM respects container limits
# -XX:MaxRAMPercentage=75.0: prevents JVM from using too much memory - useful for clouds with low RAM
# -jar app.jar: runs the app
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]