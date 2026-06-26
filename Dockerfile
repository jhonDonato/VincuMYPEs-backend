# ============================================
# ETAPA 1: BUILD
# ============================================
FROM openjdk:21-jdk-slim AS build

# Instalar Maven
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# Establecer directorio de trabajo
WORKDIR /app

# Copiar pom.xml y descargar dependencias (caché)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copiar código fuente
COPY src src
COPY .env .env

# Construir JAR
RUN mvn clean package -DskipTests

# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM openjdk:21-jdk-slim

# Instalar herramientas
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Crear usuario no root
RUN groupadd -r mypelink && useradd -r -g mypelink mypelink

# Crear directorios necesarios
RUN mkdir -p /app/logs && chown -R mypelink:mypelink /app

# Establecer directorio de trabajo
WORKDIR /app

# Copiar JAR desde build
COPY --from=build /app/target/*.jar app.jar

# Copiar .env (si existe)
COPY .env .env

# Cambiar propietario
RUN chown mypelink:mypelink app.jar

# Cambiar a usuario no root
USER mypelink

# Puerto de la aplicación
EXPOSE 8080

# Variables de entorno
ENV SPRING_PROFILES_ACTIVE=aws
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

# Healthcheck
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Comando de entrada
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]