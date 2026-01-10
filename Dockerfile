# Estágio 1: Build com imagem do maven e JDK 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

COPY pom.xml .
# Se mudar código Java mas n mudar o pom.xml, o Docker reutiliza essa camada e n baixa tudo de novo
RUN mvn dependency:go-offline

# Copia o código e gera o JAR
COPY src ./src
RUN mvn clean package -DskipTests

# Estágio 2: Runtime
# Utiliza uma imagem mais leve apenas com JRE
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
# Copia apenas o JAR gerado no estágio anterior
COPY --from=build /app/target/*.jar app.jar

# Expõe a porta padrão do Spring Boot
EXPOSE 8080

# Configurações de memória otimizadas para containers pequenos
# -XX:+UseContainerSupport: JVM respeita limites do container
# -XX:MaxRAMPercentage=75.0: evita a JVM pegar memória demais - caso eu use clouds com pouca RAM
# -jar app.jar: executa a app
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]