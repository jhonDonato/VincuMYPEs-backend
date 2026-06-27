# ============================================
# ETAPA 1: BUILD
# ============================================
FROM eclipse-temurin:21-jdk-alpine AS build

# Instalar Maven
RUN apk add --no-cache maven

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B

COPY src src
COPY .env.aws .env

RUN mvn clean package -DskipTests -Dmaven.test.skip=true

# ============================================
# ETAPA 2: RUNTIME
# ============================================
FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache curl
RUN addgroup -g 1000 -S mypelink && adduser -u 1000 -S mypelink -G mypelink
RUN mkdir -p /app/logs && chown -R mypelink:mypelink /app

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar
COPY .env.aws .env
RUN chown mypelink:mypelink app.jar

USER mypelink

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=aws
ENV JAVA_OPTS="-Xmx512m -Xms256m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]