# ===============================
# Etapa 1: Compilación con Maven
# ===============================
FROM maven:3.9.6-eclipse-temurin-21 AS builder

# Directorio de trabajo dentro del contenedor
WORKDIR /app

# Copiamos los ficheros necesarios para resolver dependencias
COPY pom.xml .
COPY application ./application
COPY boot ./boot
COPY devops ./devops
COPY driven ./driven
COPY driving ./driving
COPY mvnw .
COPY mvnw.cmd .
COPY .mvn ./.mvn

# Descargamos dependencias en caché
RUN mvn dependency:go-offline -B

# Compilamos el proyecto (asumiendo que el JAR se genera en boot/target)
RUN mvn clean package -DskipTests

# ===============================
# Etapa 2: Imagen final ligera
# ===============================
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copiamos el JAR compilado desde la etapa anterior
COPY --from=builder /app/boot/target/boot-0.0.1-SNAPSHOT.jar app.jar

# Puerto que expone tu aplicación Spring Boot
EXPOSE 8080

# Comando de arranque
ENTRYPOINT ["java", "-jar", "app.jar"]
