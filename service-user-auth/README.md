# service-user-auth

Microservicio de autenticación de Gestión de Requerimientos. Valida el
usuario de dominio (Windows) contra la base de datos y devuelve sus datos
básicos y roles activos. Es independiente de `service-requerimiento` (otro
puerto, otro proceso), pero usa las mismas bases de datos SQL Server
(GestionRQ / organizacion), ya que reutiliza los stored procedures ya
existentes.

## Endpoints

- `GET /api/auth/login/{usuarioWindows}` — inicia sesión. Usa
  `spGR_Seguridad_ConsultarUsuario` (siTipBus = 1), que internamente llama a
  `organizacion..spEO_Personal_BuscarPersonaxUsuWin`. Responde `401` si el
  usuario no está registrado en Gestión de Requerimientos y `403` si no
  tiene roles activos asignados.

- `GET /api/auth/roles/{usuarioWindows}?codigoRol=0` — lista los roles
  activos del usuario junto con sus datos de persona
  (`spGR_Seguridad_ConsultarUsuario`, siTipBus = 2).

## Configuración

Copia `application-template.properties` a `application.properties` (este
último no se versiona) y completa la cadena de conexión. Por defecto corre
en el puerto `8081` para no chocar con `service-requerimiento` (`8080`).

## Ejecutar

```
./mvnw spring-boot:run
```
