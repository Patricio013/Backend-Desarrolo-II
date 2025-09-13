# ---------- Build stage ----------
FROM maven:3.9.8-eclipse-temurin-21 AS builder
WORKDIR /app

COPY pom.xml .
RUN mvn -q -e -B -DskipTests dependency:go-offline

COPY src ./src
RUN mvn -q -e -B -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/target/*SNAPSHOT.jar /app/app.jar

EXPOSE 80
ENV SERVER_PORT=80

ENTRYPOINT ["java","-jar","/app/app.jar"]
