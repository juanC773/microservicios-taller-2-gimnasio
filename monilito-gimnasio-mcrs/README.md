# Monolito — Sistema de Gestión de Gimnasio

## ¿Qué es este proyecto?

Este proyecto es la **aplicación monolítica original** del sistema de gestión de gimnasio. Fue el punto de partida del ejercicio de refactorización hacia una arquitectura de microservicios.

En una arquitectura monolítica, todos los módulos de la aplicación conviven en un único proceso, comparten una sola base de datos y se despliegan juntos como una unidad.

## Dominio

El sistema gestiona cuatro entidades dentro de un único `GimnasioService`:

| Entidad      | Descripción                                           |
| ------------ | ----------------------------------------------------- |
| `Miembro`    | Persona inscrita al gimnasio (nombre, email, fecha)   |
| `Entrenador` | Instructor del gimnasio (nombre, especialidad)        |
| `Clase`      | Sesión programada (horario, capacidad, entrenador FK) |
| `Equipo`     | Equipamiento disponible (nombre, descripción, stock)  |

## Estructura del proyecto

```
src/main/java/co/analisys/gimnasio/
├── GimnasioApplication.java       — Punto de entrada
├── DataLoader.java                — Datos de ejemplo al arrancar
├── controller/
│   └── GimnasioController.java    — Todos los endpoints bajo /api/gimnasio
├── model/
│   ├── Miembro.java
│   ├── Entrenador.java
│   ├── Clase.java                 — Contiene @ManyToOne Entrenador
│   └── Equipo.java
├── repository/
│   ├── MiembroRepository.java
│   ├── EntrenadorRepository.java
│   ├── ClaseRepository.java
│   └── EquipoRepository.java
└── service/
    └── GimnasioService.java       — Toda la lógica centralizada aquí
```

## Endpoints

Todos los endpoints están centralizados bajo `/api/gimnasio`:

| Método | Ruta                         | Descripción         |
| ------ | ---------------------------- | ------------------- |
| POST   | `/api/gimnasio/miembros`     | Registrar miembro   |
| GET    | `/api/gimnasio/miembros`     | Listar miembros     |
| POST   | `/api/gimnasio/entrenadores` | Agregar entrenador  |
| GET    | `/api/gimnasio/entrenadores` | Listar entrenadores |
| POST   | `/api/gimnasio/clases`       | Programar clase     |
| GET    | `/api/gimnasio/clases`       | Listar clases       |
| POST   | `/api/gimnasio/equipos`      | Agregar equipo      |
| GET    | `/api/gimnasio/equipos`      | Listar equipos      |

## Ejecución

```bash
bash mvnw spring-boot:run
```

Puerto: `8080` | Base de datos: `jdbc:h2:mem:gimnasiodb`

## Por qué se refactorizó

La relación `@ManyToOne` entre `Clase` y `Entrenador`, y el hecho de que `GimnasioService` acumule responsabilidades de cuatro dominios distintos, son señales claras de que el sistema puede beneficiarse de una descomposición. Ver la versión descompuesta en los proyectos `microservicio-*`
