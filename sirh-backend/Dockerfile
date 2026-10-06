# ==========================================
# ÉTAPE 1 : BUILD
# ==========================================
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw

RUN ./mvnw dependency:go-offline -B

COPY src src

RUN ./mvnw clean package -DskipTests


# ==========================================
# ÉTAPE 2 : RUNTIME
# ==========================================
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads

EXPOSE 10000

ENTRYPOINT ["sh", "-c", "java -Dserver.port=${PORT:-10000} -Dserver.address=0.0.0.0 -jar app.jar"]