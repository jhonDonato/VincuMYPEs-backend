-- ==========================================
-- INICIALIZACIÓN DE BASE DE DATOS
-- ==========================================

-- Configurar charset
SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- Crear base de datos si no existe
CREATE DATABASE IF NOT EXISTS mypelink_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mypelink_db;

-- Configuración adicional
SET GLOBAL max_allowed_packet = 67108864;  -- 64MB
SET GLOBAL innodb_buffer_pool_size = 268435456;  -- 256MB
SET GLOBAL time_zone = 'America/Lima';

-- Mostrar configuración
SELECT 'Base de datos inicializada correctamente' AS Mensaje;