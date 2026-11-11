# ---------- Etapa 1: compilar ----------
FROM maven:3.9-eclipse-temurin-21 AS compilar
WORKDIR /app

COPY pom.xml .
RUN mvn -q -B dependency:go-offline

COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Etapa 2: ejecutar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN useradd --system --uid 1001 eternal
COPY --from=compilar /app/target/eternalsunshine-web-0.0.1-SNAPSHOT.jar app.jar
USER eternal

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=70", "-jar", "/app/app.jar"]