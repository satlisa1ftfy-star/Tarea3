# Gestión de Requerimientos - Backend

API REST desarrollada con Spring Boot para la gestión y consulta de requerimientos.

El proyecto permite consultar información de requerimientos, personas, unidades orgánicas, categorías, subcategorías, estados y roles mediante procedimientos almacenados de SQL Server.

## Microservicios

| Servicio | Puerto | Base path | Responsabilidad |
|---|---|---|---|
| `service-user-auth` | 8081 | `/api/auth` | Login (usuario Windows), emisión del JWT, roles y registro de sesión |
| `service-organizacion` | 8082 | `/api/organizacion` | Búsqueda de unidades orgánicas y personas |
| `service-requerimiento` | 8080 | `/api/requerimiento` | Requerimientos, categorías, subcategorías y estados |

`service-organizacion` y `service-requerimiento` validan el mismo JWT: `JWT_SECRET` debe ser igual en los tres servicios.

## Tecnologías

- Java 17
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- SQL Server
- Lombok
- Swagger / OpenAPI
- Maven

## Arquitectura

La aplicación sigue la siguiente estructura:

Controller → Service → Repository → Stored Procedure → SQL Server

### Controller

Expone los endpoints REST.

### Service

Realiza validaciones y normalización de parámetros.

Ejemplo:

- `0`: no aplicar filtro numérico.
- `TODO`: no aplicar filtro de texto.
- `null`: se convierte al valor general correspondiente.

### Repository

Se encarga de:

- Generar el XML solicitado por los procedimientos almacenados.
- Ejecutar los Stored Procedures.
- Mapear los resultados SQL hacia los modelos Java.

La lógica de búsqueda y filtrado se mantiene principalmente en los procedimientos almacenados.

<img width="1920" height="1080" alt="image" src="https://github.com/user-attachments/assets/72d18263-54e1-4849-83cf-5a2d3210bb99" />


## Base de datos

Motor:

```text
SQL Server


