# Etapa de build
FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q clean package -DskipTests

# Etapa de runtime
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S maintena && adduser -S maintena -G maintena
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/uploads \
    && chown -R maintena:maintena /app/app.jar /app/uploads
USER maintena
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
