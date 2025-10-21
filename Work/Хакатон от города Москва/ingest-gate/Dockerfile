# Этап сборки
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /app

# Скопировать pom.xml и скачать зависимости
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Скопировать исходники и собрать jar
COPY src ./src
RUN mvn clean package -DskipTests

# Этап запуска
FROM eclipse-temurin:17-jdk
WORKDIR /app

# Копируем собранный jar
COPY --from=build /app/target/*.jar app.jar

# Копируем ресурсы (Excel файлы)
COPY src/main/resources/*.xlsx ./resources/


ENTRYPOINT ["java", "-jar", "app.jar"]
