# ===== BUILD STAGE =====
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# copy file cấu hình + source
COPY pom.xml .
COPY src ./src

# build jar
RUN mvn clean package -DskipTests

# ===== RUNTIME STAGE =====
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# copy file jar từ stage build sang
COPY --from=build /app/target/*.jar app.jar

# Spring Boot sẽ dùng biến PORT, mình expose 8080 cho dễ test local
EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
