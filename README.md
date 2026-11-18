# AgroCiclo

Sistema web de gestión agrícola inteligente desarrollado con Angular, Java y Spring Boot.

## Tecnologías

- Frontend: Angular
- Backend: Java 17 + Spring Boot
- Base de datos: PostgreSQL
- API REST
- Tailscale para publicación temporal durante las pruebas
- Git y GitHub

## Funcionalidades

- Inicio de sesión y autenticación
- Gestión de usuarios y roles
- Gestión de terrenos
- Gestión de cultivos y ciclos agrícolas
- Registro de actividades
- Registro de insumos y movimientos
- Registro de cosechas
- Indicadores agrícolas
- Notificaciones
- Reportes
- Asistente de inteligencia agrícola
- Verificación y recuperación de cuentas

## Base de datos

Las migraciones SQL se encuentran en:

`backend/src/main/resources/db/migrations/`

Incluyen el esquema inicial y las modificaciones posteriores de la base de datos.

Los datos ficticios para demostración se encuentran en:

`backend/src/main/resources/db/demo.sql`

## Configuración

Las credenciales y variables sensibles no se incluyen en el repositorio.

Se proporciona:

`backend/src/main/resources/application-example.properties`

y:

`frontend/.env.example`

## Publicación

Durante las pruebas se utilizó Tailscale para publicar temporalmente el frontend y la API.

El proyecto fue probado mediante acceso web y conexión entre frontend, backend y PostgreSQL.

## Repositorio

https://github.com/kevincrespo2017-ui/AgroCiclo

## Estado

Proyecto funcional y preparado para demostración, sustentación y entrega académica.
