# Eternal Sunshine

Sistema de control de inventario desde la nube para supermercados locales y pequeños negocios. Proyecto de la Práctica de Ingeniería V, Ingeniería de Sistemas, Universidad Central (Bogotá).

- Autor: Sergio Alejandro Quemba Moreno
- Sistema en línea: https://eternalsunshine.onrender.com
- Rama principal de la entrega: `multitienda`

El sistema está desplegado en Render con una base de datos PostgreSQL administrada en Neon. Cualquier persona puede crear su propia tienda desde la opción de registro de la pantalla de inicio de sesión; los datos de cada tienda quedan separados de los de las demás. La versión gratuita de Render se suspende tras un tiempo sin uso, así que la primera carga puede tardar unos segundos.

## Qué incluye

- Inventario de productos con código, categoría, precio y stock, y búsqueda mientras se escribe.
- Roles Administrador y Empleado, con permisos validados en el servidor. El empleado solicita crear o editar productos y el administrador aprueba o rechaza.
- Ventas con descuento de stock, comprobante imprimible y anulación por el administrador.
- Recepción de mercancía con proveedor y factura, y anulación de entradas.
- Reabastecimiento sugerido a partir del promedio de ventas de los últimos 30 días.
- Conteo físico de inventario con cámara (código de barras EAN-13).
- Historial de movimientos con filtros y exportación del inventario a Excel y PDF con clasificación ABC.
- Dashboard con gráficas, multitienda con datos aislados, gestión de usuarios y recuperación de contraseña por correo.

## Tecnologías

Java 21, Spring Boot 3.5, Spring MVC, Thymeleaf, Spring Data JPA (Hibernate), PostgreSQL, Maven, Apache POI y OpenPDF para exportar, Quagga2 para el escáner, Chart.js para las gráficas, Docker para el despliegue, Render, Neon y Brevo (correo).

## Estructura

```
src/main/java/com/eternalsunshine/web/
  controller/   Controladores MVC
  service/      Lógica de negocio
  repository/   Acceso a datos (Spring Data JPA)
  entity/       Entidades; EntidadDeTienda es la base del aislamiento entre tiendas
  config/       Seguridad, interceptores, filtros y arranque inicial
src/main/resources/templates/   Vistas Thymeleaf
src/main/resources/static/      CSS, JavaScript e imágenes
src/test/java/                  Pruebas unitarias (JUnit 5 y Mockito)
Dockerfile                      Imagen de dos etapas (Maven y luego Java 21)
```

## Ejecutarlo en local

Requisitos: Java 21 y PostgreSQL.

1. Crear una base de datos vacía, por ejemplo `inventario_db`, y un usuario con permisos sobre ella.
2. Definir las variables de entorno (ver la tabla de abajo). Como mínimo `DATABASE_PASSWORD`, `ADMIN_USERNAME` y `ADMIN_PASSWORD`.
3. Iniciar la aplicación:

```
./mvnw spring-boot:run
```

En Windows: `mvnw.cmd spring-boot:run`. La aplicación queda en http://localhost:8080. Las tablas se crean solas al primer arranque y se crea el administrador inicial con el usuario y la clave definidos en `ADMIN_USERNAME` y `ADMIN_PASSWORD`.

## Variables de entorno

La aplicación no guarda claves en el código. Los valores se entregan por variables de entorno.

| Variable | Para qué sirve | Por defecto |
|---|---|---|
| `DATABASE_URL` | URL JDBC de PostgreSQL | `jdbc:postgresql://localhost:5432/inventario_db` |
| `DATABASE_USERNAME` | Usuario de la base de datos | `inventario_user` |
| `DATABASE_PASSWORD` | Clave de la base de datos | (obligatoria) |
| `ADMIN_USERNAME`, `ADMIN_PASSWORD` | Administrador inicial | vacío (no se crea) |
| `ADMIN_TIENDA` | Nombre de la tienda del administrador inicial | `Tienda principal` |
| `COOKIE_SECURE` | `true` en producción para que la cookie solo viaje por HTTPS | `false` |
| `APP_URL` | Dirección pública, usada en los enlaces de correo | vacío |
| `BREVO_API_KEY`, `MAIL_FROM`, `MAIL_FROM_NAME` | Envío del correo de recuperación de contraseña | vacío |
| `REGISTRO_HABILITADO` | Permite o cierra el registro público de tiendas | `true` |
| `MAX_TIENDAS` | Límite de tiendas registradas | `200` |
| `SHOW_SQL` | Muestra las consultas SQL en consola | `false` |
| `PORT` | Puerto del servidor | `8080` |

Sin `BREVO_API_KEY` el sistema funciona igual; solo la recuperación de contraseña por correo queda sin enviar.

## Pruebas

Las 75 pruebas unitarias no necesitan base de datos:

```
./mvnw test -Dtest="*ServiceTest,NumeracionTest,UsuarioPasswordTest"
```

Los 40 casos de prueba manuales están en la matriz del capítulo 19 del documento.

## Despliegue con Docker

```
docker build -t eternalsunshine .
docker run -p 8080:8080 -e DATABASE_URL=... -e DATABASE_USERNAME=... -e DATABASE_PASSWORD=... -e ADMIN_USERNAME=... -e ADMIN_PASSWORD=... eternalsunshine
```

En Render el servicio se despliega desde este repositorio con el `Dockerfile`, y cada cambio subido a la rama `multitienda` lo vuelve a desplegar.

## Seguridad en breve

Contraseñas con BCrypt; bloqueo tras 5 intentos fallidos; roles validados en el servidor; verificación de origen en las peticiones que modifican datos; separación de datos por tienda; enlaces de recuperación de un solo uso con vencimiento; sesión que expira por inactividad; cookies HttpOnly y SameSite; cabeceras CSP y HSTS; claves únicamente por variables de entorno.