#!/bin/bash

# ==========================================
# SCRIPT DE DESPLIEGUE PARA AWS EC2
# ==========================================

set -e

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}🚀 DESPLIEGUE MYPELINK EN AWS EC2${NC}"
echo -e "${BLUE}========================================${NC}"

# ==========================================
# 1. VERIFICAR ENTORNO
# ==========================================
echo -e "${YELLOW}📋 Verificando entorno...${NC}"

# Verificar Docker
if ! command -v docker &> /dev/null; then
    echo -e "${RED}❌ Docker no está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker instalado${NC}"

# Verificar Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo -e "${RED}❌ Docker Compose no está instalado${NC}"
    exit 1
fi
echo -e "${GREEN}✅ Docker Compose instalado${NC}"

# ==========================================
# 2. CARGAR VARIABLES DE ENTORNO
# ==========================================
echo -e "${YELLOW}📋 Cargando variables de entorno...${NC}"

if [ ! -f .env.aws ]; then
    echo -e "${RED}❌ Archivo .env.aws no encontrado${NC}"
    exit 1
fi

export $(grep -v '^#' .env.aws | xargs)

echo -e "${GREEN}✅ Variables de entorno cargadas${NC}"

# ==========================================
# 3. CONSTRUIR APLICACIÓN
# ==========================================
echo -e "${YELLOW}📦 Construyendo aplicación con Maven...${NC}"

# Opción 1: Usar Maven local
# ./mvnw clean package -DskipTests

# Opción 2: Usar Maven en Docker (más confiable)
docker run --rm \
  -v "$(pwd)":/app \
  -v "$HOME/.m2":/root/.m2 \
  -w /app \
  maven:3.9-eclipse-temurin-21 \
  mvn clean package -DskipTests

echo -e "${GREEN}✅ Aplicación construida${NC}"

# ==========================================
# 4. CONSTRUIR IMAGEN DOCKER
# ==========================================
echo -e "${YELLOW}🐳 Construyendo imagen Docker...${NC}"

docker build -t mypelink-backend:latest .

echo -e "${GREEN}✅ Imagen Docker construida${NC}"

# ==========================================
# 5. DETENER CONTENEDORES EXISTENTES
# ==========================================
echo -e "${YELLOW}🛑 Deteniendo contenedores existentes...${NC}"

docker-compose -f docker-compose-aws.yml down 2>/dev/null || true

echo -e "${GREEN}✅ Contenedores detenidos${NC}"

# ==========================================
# 6. CREAR DIRECTORIOS NECESARIOS
# ==========================================
echo -e "${YELLOW}📁 Creando directorios...${NC}"

mkdir -p docker/ssl logs

echo -e "${GREEN}✅ Directorios creados${NC}"

# ==========================================
# 7. LEVANTAR CONTENEDORES
# ==========================================
echo -e "${YELLOW}🚀 Levantando contenedores...${NC}"

docker-compose -f docker-compose-aws.yml up -d

echo -e "${GREEN}✅ Contenedores levantados${NC}"

# ==========================================
# 8. VERIFICAR ESTADO
# ==========================================
echo -e "${YELLOW}🔍 Verificando estado de contenedores...${NC}"

sleep 10

docker-compose -f docker-compose-aws.yml ps

# ==========================================
# 9. VERIFICAR HEALTHCHECK
# ==========================================
echo -e "${YELLOW}🏥 Verificando healthcheck del backend...${NC}"

max_attempts=30
attempt=0

while [ $attempt -lt $max_attempts ]; do    if docker-compose -f docker-compose-aws.yml exec backend curl -f http://localhost:8080/actuator/health 2>/dev/null; then
        echo -e "${GREEN}✅ Backend saludable${NC}"
        break
    fi
    attempt=$((attempt + 1))
    echo -e "${YELLOW}⏳ Esperando backend... (${attempt}/${max_attempts})${NC}"
    sleep 5
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}❌ Backend no responde${NC}"
    echo -e "${YELLOW}📋 Logs del backend:${NC}"
    docker-compose -f docker-compose-aws.yml logs --tail=50 backend
    exit 1
fi

# ==========================================
# 10. MOSTRAR LOGS
# ==========================================
echo -e "${BLUE}========================================${NC}"
echo -e "${GREEN}🎉 ¡DESPLIEGUE COMPLETADO!${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e ""
echo -e "${GREEN}📊 Servicios disponibles:${NC}"
echo -e "  🌐 API: https://api.mypelink.com"
echo -e "  🗄️ phpMyAdmin: https://api.mypelink.com:8081"
echo -e "  📊 Healthcheck: https://api.mypelink.com/actuator/health"
echo -e ""
echo -e "${YELLOW}📋 Comandos útiles:${NC}"
echo -e "  docker-compose -f docker-compose-aws.yml logs -f    # Ver logs en tiempo real"
echo -e "  docker-compose -f docker-compose-aws.yml ps         # Ver estado de contenedores"
echo -e "  docker-compose -f docker-compose-aws.yml down       # Detener servicios"
echo -e "  docker-compose -f docker-compose-aws.yml exec backend bash # Acceder al contenedor"
echo -e ""
echo -e "${BLUE}========================================${NC}"