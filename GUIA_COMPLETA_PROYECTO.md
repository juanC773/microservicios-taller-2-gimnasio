# Guía completa: dominar el proyecto de microservicios del gimnasio

Esta guía explica **cómo funciona todo** el proyecto para que puedas dominarlo al 100%. Asume que ya sabes que son microservicios; aquí verás la arquitectura, el código y los flujos concretos.

---

## 1. Qué es este proyecto en una frase

Es un **sistema de gestión de gimnasio** que en lugar de ser una sola aplicación (monolito) está dividido en **varios servicios independientes** que se registran en un **servidor de descubrimiento (Eureka)** y se comunican por HTTP cuando hace falta (por ejemplo, Clases comprueba que el entrenador exista antes de guardar).

---

## 2. Los 5 componentes del sistema

Hay **5 aplicaciones** (cada una es un proceso Java independiente):

| # | Carpeta / Aplicación | Puerto | Rol |
|---|----------------------|--------|-----|
| 1 | `servidor-descubrimiento-mcrs` | **8761** | Servidor Eureka: “directorio” donde los demás se registran y se descubren. |
| 2 | `microservicio-entrenadores-mcrs` | **8083** | CRUD de entrenadores + endpoint “¿existe este id?”. |
| 3 | `microservicio-miembros-mcrs` | **8081** | CRUD de miembros del gimnasio. |
| 4 | `microservicio-equipos-mcrs` | **8084** | CRUD de equipamiento (inventario). |
| 5 | `microservicio-clases-mcrs` | **8082** | CRUD de clases; al crear una clase valida el entrenador (Entrenadores); al inscribir un miembro valida que exista y tenga membresía activa (Miembros). |

El **monolito** (`monilito-gimnasio-mcrs`) es la versión antigua: todo en una sola app y una sola base de datos. Los microservicios son la evolución: un servicio por dominio, cada uno con su propia BD.

---

## 3. Por qué existe Eureka y cómo funciona

### Problema sin Eureka

Si el servicio de **Clases** tuviera que llamar al de **Entrenadores** con la URL fija:

- `http://localhost:8083/entrenadores/1/existe`

tendrías **acoplamiento** al puerto y al host. Si cambias el puerto de Entrenadores o despliegas en otro servidor, tendrías que tocar código o configuración en Clases.

### Solución: descubrimiento por nombre

Con Eureka:

1. **Cada microservicio** (Entrenadores, Miembros, Equipos, Clases) al arrancar se **registra** en Eureka diciendo: “Soy el servicio X y estoy en `host:puerto`”.
2. **Clases** no usa `localhost:8083`, sino el **nombre lógico** del servicio: `ENTRENADOR-SERVICE`.
3. Spring Cloud (con `@LoadBalanced RestTemplate`) **pregunta a Eureka**: “¿Dónde está ENTRENADOR-SERVICE?” y Eureka responde con la URL real (por ejemplo `http://localhost:8083`).
4. La llamada se hace a esa URL. Si mañana Entrenadores corre en otro puerto o en otra máquina, solo cambia el registro en Eureka; el código de Clases sigue usando `http://ENTRENADOR-SERVICE/...`.

En resumen: **Eureka es el “directorio”**: los servicios se registran y los clientes resuelven el nombre del servicio a una URL real.

### Dónde está esto en el código

- **Servidor Eureka**  
  - `servidor-descubrimiento-mcrs`:  
    - `@EnableEurekaServer` en `DescubrimientoApplication.java`.  
    - `application.properties`: puerto 8761, y que este servidor no se registre a sí mismo en Eureka (`register-with-eureka=false`, `fetch-registry=false`).

- **Cada microservicio (cliente)**  
  - En el `pom.xml`: dependencia `spring-cloud-starter-netflix-eureka-client`.  
  - En `application.properties`:  
    - `eureka.client.serviceUrl.defaultZone=http://localhost:8761/eureka/`  
    - `spring.application.name=entrenador-service` (o `clase-service`, etc.).  
  Eureka muestra los nombres en mayúsculas (p. ej. `ENTRENADOR-SERVICE`); ese es el nombre que se usa en las URLs de llamada entre servicios.

- **Clase-service** usa los nombres en `ClaseService.java`:
  - `"http://ENTRENADOR-SERVICE/entrenadores/" + ... + "/existe"` (al crear clase).
  - `"http://MIEMBRO-SERVICE/miembros/" + ... + "/puede-asistir-clase"` (al inscribir miembro).
  - En `ClaseServiceApplication.java` hay un `@Bean` de `RestTemplate` con `@LoadBalanced`, que resuelve esos nombres vía Eureka.

---

## 4. Comunicaciones entre microservicios: Clases → Entrenadores y Clases → Miembros

En este proyecto hay **dos llamadas entre servicios**, ambas desde el servicio de Clases:

### 4.1 Clases → Entrenadores

- **Cuándo:** al **programar una nueva clase** (POST a `/clases`).  
- **Para qué:** comprobar que el `entrenadorId` de la clase corresponda a un entrenador que **existe** en el servicio de Entrenadores.

### 4.2 Clases → Miembros

- **Cuándo:** al **inscribir un miembro en una clase** (POST a `/clases/{claseId}/miembros`).  
- **Para qué:** comprobar que el miembro **existe** y tiene **membresía activa** (atributo `membresiaActiva` en el servicio de Miembros). Si no, se lanza `MiembroNoPuedeAsistirException`.

### Flujo paso a paso

1. El cliente hace `POST /clases` con un JSON que incluye `entrenadorId` (por ejemplo `"1"`).
2. `ClaseController.programarClase(clase)` recibe el cuerpo y llama a `ClaseService.programarClase(clase)`.
3. `ClaseService` hace una petición HTTP:
   - URL: `http://ENTRENADOR-SERVICE/entrenadores/1/existe`
   - Método: GET.
   - Con `@LoadBalanced`, Spring resuelve `ENTRENADOR-SERVICE` a la instancia registrada (p. ej. `http://localhost:8083`).
4. En **entrenador-service**, `EntrenadorController` tiene `@GetMapping("/{id}/existe")` que devuelve `true` o `false`.
5. Si la respuesta es `false` o null, `ClaseService` lanza `EntrenadorNoEncontradoException` (error 500 con mensaje claro).
6. Si es `true`, `ClaseService` hace `claseRepository.save(clase)` en su propia base de datos (`clasesdb`).

Así, **Clases** no tiene una base de datos de entrenadores ni una entidad `Entrenador`: solo guarda el **ID** del entrenador y delega la existencia en el otro servicio.

**Flujo inscribir miembro en una clase:** el cliente hace `POST /clases/{claseId}/miembros` con body `{"miembroid_value": "1"}`. `ClaseService.inscribirMiembro` llama a `GET http://MIEMBRO-SERVICE/miembros/1/puede-asistir-clase`; si devuelve `true` (miembro existe y `membresiaActiva` es true), añade el `MiembroId` a la clase y guarda. Si no, lanza `MiembroNoPuedeAsistirException`. También se comprueba que no se supere la capacidad máxima de la clase.

Eso es desacoplamiento entre contextos (DDD).

---

## 5. Bases de datos: una por microservicio

Cada microservicio tiene **su propia base de datos en memoria (H2)**:

| Servicio     | Base de datos (H2) |
|-------------|--------------------|
| Miembros    | `jdbc:h2:mem:miembrosdb` |
| Clases      | `jdbc:h2:mem:clasesdb`   |
| Entrenadores| `jdbc:h2:mem:entrenadoresdb` |
| Equipos     | `jdbc:h2:mem:equiposdb`  |

Ningún servicio accede a la BD de otro. Las relaciones entre dominios (Clase ↔ Entrenador, Clase ↔ Miembros) se resuelven por **referencia por ID** y llamadas HTTP, no por claves foráneas entre BBDD.

---

## 6. DDD en el proyecto: agregados y value objects

El análisis (ANALISIS.md) describe el diseño con **Domain-Driven Design**:

- **Bounded context:** cada microservicio es un contexto (Miembros, Entrenadores, Clases, Equipos).
- **Agregado:** en cada contexto hay un agregado con una raíz (p. ej. `Clase`, `Entrenador`, `Miembro`, `Equipo`).
- **Value objects:** identidades y conceptos del dominio se modelan como objetos embebidos (p. ej. `ClaseId`, `EntrenadorId`, `Email`, `Especialidad`).

Ejemplo en **Clase** (servicio de Clases):

- `Clase` es la entidad raíz del agregado.
- Tiene `ClaseId` (identidad), `EntrenadorId` (referencia a Entrenadores) y `miembrosInscritos` (conjunto de `MiembroId`, referencia a Miembros).
- **No** tiene un objeto `Entrenador`; solo `EntrenadorId` (String o value object con `entrenadorid_value`).
- La columna en BD es algo como `entrenador_id` (mapeada con `@AttributeOverride`).

Eso es lo que en el análisis se llama “romper el @ManyToOne”: en el monolito Clase tenía una relación JPA con Entrenador; en microservicios Clase solo guarda el ID y valida existencia vía REST.

---

## 7. Estructura típica de cada microservicio

Cada uno sigue una estructura similar:

- **`XxxApplication.java`**  
  - `@SpringBootApplication`, método `main`, y en Clase-service además el `@Bean` de `RestTemplate` con `@LoadBalanced`.

- **`controller/`**  
  - `@RestController`, `@RequestMapping("/entrenadores" | "/miembros" | "/equipos" | "/clases")`, métodos GET/POST/PUT que delegan en el servicio.

- **`service/`**  
  - Lógica de negocio; en `ClaseService` está la llamada con `RestTemplate` a `ENTRENADOR-SERVICE`.

- **`repository/`**  
  - Interfaz `JpaRepository<Entidad, Id>` para persistir en la BD del servicio.

- **`model/`**  
  - Entidades JPA y value objects (`@Embeddable`, `@EmbeddedId`, etc.).

- **`exception/`**  
  - Por ejemplo `EntrenadorNoEncontradoException` en clase-service, manejada para devolver el error adecuado.

- **`application.properties`**  
  - `spring.application.name`, `server.port`, URL de Eureka, JDBC H2, etc.

---

## 8. Orden de arranque y verificación

1. Arrancar **primero** el servidor Eureka (puerto 8761).  
2. Luego los cuatro microservicios (recomendable arrancar Entrenadores antes que Clases para poder probar la comunicación de inmediato).  
3. Abrir `http://localhost:8761` y comprobar que aparecen registrados:  
   `ENTRENADOR-SERVICE`, `MIEMBRO-SERVICE`, `EQUIPO-SERVICE`, `CLASE-SERVICE`.  
4. Probar con los curls de la `GUIA_EJECUCION.md` (listar, crear, y en Clases crear una clase con `entrenadorId` válido y luego con uno inexistente para ver el error).

Las consolas H2 de cada servicio permiten inspeccionar cada BD por separado (ver `GUIA_EJECUCION.md` para URLs y JDBC).

---

## 9. Resumen para dominar el proyecto

- **5 procesos:** 1 Eureka + 4 microservicios (Entrenadores, Miembros, Equipos, Clases).  
- **Eureka:** registro y descubrimiento por nombre; los clientes usan `http://NOMBRE-SERVICIO/...` y Spring resuelve con `@LoadBalanced RestTemplate`.  
- **Dos llamadas entre servicios:** Clases → Entrenadores `/{id}/existe` al crear clase; Clases → Miembros `/{id}/puede-asistir-clase` al inscribir un miembro en una clase.  
- **Una BD por servicio;** Clase guarda solo `entrenadorId`, no el objeto Entrenador.  
- **DDD:** agregados, value objects, referencias por ID entre contextos.  
- **Orden de arranque:** Eureka primero, luego los cuatro servicios; recomendable Entrenadores antes que Clases.

Con esto tienes el mapa completo del proyecto: qué hace cada pieza, por qué está Eureka, cómo es la comunicación entre servicios y cómo se refleja en el código y en la ejecución.
