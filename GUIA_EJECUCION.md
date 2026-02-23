# Guía de Ejecución — Sistema de Gestión de Gimnasio (Microservicios)

## Prerequisitos

- Java 17+
- Cinco terminales abiertas (una por servicio)

> Los proyectos incluyen el Maven Wrapper (`mvnw`), por lo que **no se necesita Maven instalado globalmente**.

---

## Orden de arranque

El servidor Eureka **debe estar corriendo antes** que cualquier microservicio. Los cuatro microservicios pueden arrancarse en cualquier orden entre sí, pero se recomienda arrancar primero `entrenadores` para que esté registrado en Eureka cuando se pruebe la comunicación desde `clases`.

```
1. servidor-descubrimiento-mcrs   (puerto 8761)
2. microservicio-entrenadores-mcrs (puerto 8083)
3. microservicio-miembros-mcrs     (puerto 8081)
4. microservicio-equipos-mcrs      (puerto 8084)
5. microservicio-clases-mcrs       (puerto 8082)
```

---

## Comandos de ejecución

> Ejecutar cada comando en una terminal separada desde la raíz de la carpeta `entrega/`.

### Terminal 1 — Eureka Server
```bash
bash servidor-descubrimiento-mcrs/mvnw -f servidor-descubrimiento-mcrs/pom.xml spring-boot:run
```
Verificar en el navegador: [http://localhost:8761](http://localhost:8761)

---

### Terminal 2 — Servicio de Entrenadores
```bash
bash microservicio-entrenadores-mcrs/mvnw -f microservicio-entrenadores-mcrs/pom.xml spring-boot:run
```

---

### Terminal 3 — Servicio de Miembros
```bash
bash microservicio-miembros-mcrs/mvnw -f microservicio-miembros-mcrs/pom.xml spring-boot:run
```

---

### Terminal 4 — Servicio de Equipos
```bash
bash microservicio-equipos-mcrs/mvnw -f microservicio-equipos-mcrs/pom.xml spring-boot:run
```

---

### Terminal 5 — Servicio de Clases
```bash
bash microservicio-clases-mcrs/mvnw -f microservicio-clases-mcrs/pom.xml spring-boot:run
```

---

## Verificar que los servicios se registraron en Eureka

Una vez que todos están corriendo, abrir [http://localhost:8761](http://localhost:8761) y confirmar que aparecen estos cuatro servicios en la sección **"Instances currently registered with Eureka"**:

- `ENTRENADOR-SERVICE`
- `MIEMBRO-SERVICE`
- `EQUIPO-SERVICE`
- `CLASE-SERVICE`

---

## Datos de ejemplo precargados (DataLoader)

Cada servicio carga datos de ejemplo al arrancar:

| Servicio     | Datos precargados                                              |
|--------------|----------------------------------------------------------------|
| Entrenadores | id=`1` Carlos Rodríguez (Yoga), id=`2` Ana Martínez (Spinning) |
| Miembros     | id=`1` Juan Pérez, id=`2` María López                         |
| Equipos      | id=`1` Mancuernas (x20), id=`2` Bicicleta estática (x15)      |
| Clases       | id=`1` Yoga Matutino (entrenador=1), id=`2` Spinning Vespertino (entrenador=2) |

---

## Comandos curl de verificación

### Servicio de Entrenadores (puerto 8083)

**Listar todos los entrenadores:**
```bash
curl -s http://localhost:8083/entrenadores | python3 -m json.tool
```

**Obtener entrenador por id:**
```bash
curl -s http://localhost:8083/entrenadores/1 | python3 -m json.tool
```

**Verificar existencia de un entrenador:**
```bash
curl -s http://localhost:8083/entrenadores/1/existe
# Respuesta esperada: true
```

**Agregar nuevo entrenador:**
```bash
curl -s -X POST http://localhost:8083/entrenadores \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"entrenadorid_value": "3"},
    "nombre": "Luis Torres",
    "especialidad": {"especialidad_value": "CrossFit"}
  }' | python3 -m json.tool
```

**Actualizar especialidad:**
```bash
curl -s -X PUT http://localhost:8083/entrenadores/3/especialidad \
  -H "Content-Type: application/json" \
  -d '"Funcional"'
```

---

### Servicio de Miembros (puerto 8081)

**Listar todos los miembros:**
```bash
curl -s http://localhost:8081/miembros | python3 -m json.tool
```

**Obtener miembro por id:**
```bash
curl -s http://localhost:8081/miembros/1 | python3 -m json.tool
```

**Registrar nuevo miembro:**
```bash
curl -s -X POST http://localhost:8081/miembros \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"miembroid_value": "3"},
    "nombre": "Pedro García",
    "email": {"email_value": "pedro@email.com"},
    "fechaInscripcion": "2026-02-23"
  }' | python3 -m json.tool
```

**Actualizar email:**
```bash
curl -s -X PUT http://localhost:8081/miembros/3/email \
  -H "Content-Type: application/json" \
  -d '"pedro.nuevo@email.com"'
```

---

### Servicio de Equipos (puerto 8084)

**Listar todos los equipos:**
```bash
curl -s http://localhost:8084/equipos | python3 -m json.tool
```

**Obtener equipo por id:**
```bash
curl -s http://localhost:8084/equipos/1 | python3 -m json.tool
```

**Agregar nuevo equipo:**
```bash
curl -s -X POST http://localhost:8084/equipos \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"equipoid_value": "3"},
    "nombre": "Cuerda para saltar",
    "descripcion": "Cuerda de velocidad profesional",
    "cantidad": 30
  }' | python3 -m json.tool
```

**Actualizar cantidad de stock:**
```bash
curl -s -X PUT http://localhost:8084/equipos/1/cantidad \
  -H "Content-Type: application/json" \
  -d '25'
```

---

### Servicio de Clases (puerto 8082)

**Listar todas las clases:**
```bash
curl -s http://localhost:8082/clases | python3 -m json.tool
```

**Obtener clase por id:**
```bash
curl -s http://localhost:8082/clases/1 | python3 -m json.tool
```

**Programar nueva clase con entrenador válido (verifica comunicación REST con Eureka):**
```bash
curl -s -X POST http://localhost:8082/clases \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"claseid_value": "3"},
    "nombre": "CrossFit Intensivo",
    "horario": "2026-03-01T10:00:00",
    "capacidadMaxima": 10,
    "entrenadorId": {"entrenadorid_value": "1"}
  }' | python3 -m json.tool
```
> Este request activa la llamada interna: `clase-service → ENTRENADOR-SERVICE/entrenadores/1/existe`

**Intentar programar clase con entrenador inexistente (debe fallar):**
```bash
curl -s -X POST http://localhost:8082/clases \
  -H "Content-Type: application/json" \
  -d '{
    "id": {"claseid_value": "99"},
    "nombre": "Clase sin entrenador",
    "horario": "2026-03-02T09:00:00",
    "capacidadMaxima": 5,
    "entrenadorId": {"entrenadorid_value": "999"}
  }'
# Respuesta esperada: error 500 — Entrenador no encontrado con id: 999
```

---

## Consolas H2 (base de datos en memoria)

Cada microservicio expone su propia consola H2 para inspeccionar la BD directamente:

| Servicio     | URL H2 Console                        | JDBC URL               |
|--------------|---------------------------------------|------------------------|
| Miembros     | http://localhost:8081/h2-console      | `jdbc:h2:mem:miembrosdb` |
| Clases       | http://localhost:8082/h2-console      | `jdbc:h2:mem:clasesdb` |
| Entrenadores | http://localhost:8083/h2-console      | `jdbc:h2:mem:entrenadoresdb` |
| Equipos      | http://localhost:8084/h2-console      | `jdbc:h2:mem:equiposdb` |

Usuario: `sa` — Contraseña: *(vacía)*
