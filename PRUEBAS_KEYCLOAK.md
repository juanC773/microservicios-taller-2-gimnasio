# Pruebas de seguridad con Keycloak - Gimnasio

## Cómo probar todo (después de ejecutar `.\levantar-todo.ps1`)

Asegúrate de que **Keycloak** esté corriendo en `http://localhost:8080` y espera ~1 minuto a que los 5 servicios terminen de arrancar.

### Paso 1 — Obtener un token

En PowerShell, reemplaza `TU_USUARIO` y `TU_PASSWORD` por un usuario del realm **gimnasio** (con rol ROLE_TRAINER, ROLE_MEMBER o ROLE_ADMIN):

```powershell
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' -Method Post -Body @{
  grant_type    = 'password'
  client_id     = 'clase-service'
  client_secret = 'LlsQTYntkM1WHVtGzUuadRmdgwmsSoyg'
  username      = 'TU_USUARIO'
  password      = 'TU_PASSWORD'
} -ContentType 'application/x-www-form-urlencoded'

$token = $resp.access_token
Write-Host "Token guardado en `$token (primeros 50 chars): $($token.Substring(0, [Math]::Min(50, $token.Length)))..."
```

### Paso 2 — Probar endpoints públicos (sin token)

Deben responder sin pedir autenticación:

```powershell
Invoke-RestMethod -Uri 'http://localhost:8082/clases/public/status'
Invoke-RestMethod -Uri 'http://localhost:8081/miembros/public/status'
Invoke-RestMethod -Uri 'http://localhost:8083/entrenadores/public/status'
Invoke-RestMethod -Uri 'http://localhost:8084/equipos/public/status'
```

### Paso 3 — Probar endpoints protegidos (con token)

Usa el `$token` del Paso 1:

```powershell
$headers = @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri 'http://localhost:8082/clases' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8081/miembros' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8083/entrenadores' -Headers $headers
Invoke-RestMethod -Uri 'http://localhost:8084/equipos' -Headers $headers
```

### Paso 4 — Probar sin token (debe dar 401)

```powershell
try {
  Invoke-RestMethod -Uri 'http://localhost:8082/clases'
} catch {
  Write-Host "Código:" $_.Exception.Response.StatusCode.value__   # Debe ser 401
  Write-Host "Sin token correcto: acceso denegado (esperado)."
}
```

### Paso 5 — Probar con usuarios de distintos roles (opcional)

Crea en Keycloak usuarios con roles diferentes (por ejemplo uno con ROLE_MEMBER y otro con ROLE_TRAINER). Obtén un token para cada uno y prueba:

- Un **MEMBER** puede: GET /clases, GET /miembros/{id}, GET /entrenadores, GET /equipos, inscribirse en clase, etc.
- Un **TRAINER** puede además: POST /clases, POST /miembros, POST /entrenadores, POST /equipos, PUT para actualizar.

Prueba un endpoint solo permitido a TRAINER/ADMIN (por ejemplo `POST .../clases`) con un usuario MEMBER: debe devolver **403 Forbidden**.

---

## Requisitos previos

1. **Keycloak** en ejecución en `http://localhost:8080`.
2. Realm **gimnasio** con al menos un usuario y roles **ROLE_ADMIN**, **ROLE_TRAINER** o **ROLE_MEMBER** asignados.
3. En cada cliente (clase-service, miembro-service, etc.) tener habilitado **Direct access grants** si vas a usar el flujo password para obtener el token con curl.

---

## 1. Orden de arranque

1. **Servidor de descubrimiento** (puerto 8761)
2. **Microservicios** (8081, 8082, 8083, 8084) — en cualquier orden, después del descubrimiento.

---

## 2. Obtener token de Keycloak

Reemplaza `TU_USUARIO`, `TU_PASSWORD` por un usuario del realm **gimnasio** que tenga alguno de los roles (ROLE_ADMIN, ROLE_TRAINER, ROLE_MEMBER).

Puedes usar cualquier cliente; aquí se usa **clase-service**. El endpoint de token **debe ser POST** (si no, Keycloak devuelve 405).

**Git Bash / WSL / Linux** (usa `\` para continuar línea; **-X POST** obligatorio):

```bash
curl -X POST "http://localhost:8080/realms/gimnasio/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=clase-service" \
  -d "client_secret=LlsQTYntkM1WHVtGzUuadRmdgwmsSoyg" \
  -d "username=dueño" \
  -d "password=admin@2026"
```

**CMD (Windows)** (usa `^` para continuar; **-X POST** obligatorio):

```cmd
curl -X POST "http://localhost:8080/realms/gimnasio/protocol/openid-connect/token" ^
  -H "Content-Type: application/x-www-form-urlencoded" ^
  -d "grant_type=password" ^
  -d "client_id=clase-service" ^
  -d "client_secret=LlsQTYntkM1WHVtGzUuadRmdgwmsSoyg" ^
  -d "username=dueño" ^
  -d "password=admin@2026"
```

**PowerShell** (recomendado en Windows; no usa curl, evita 405):

```powershell
$resp = Invoke-RestMethod -Uri 'http://localhost:8080/realms/gimnasio/protocol/openid-connect/token' -Method Post -Body @{
  grant_type    = 'password'
  client_id     = 'clase-service'
  client_secret = 'LlsQTYntkM1WHVtGzUuadRmdgwmsSoyg'
  username      = 'dueño'
  password      = 'admin@2026'
} -ContentType 'application/x-www-form-urlencoded'
$resp.access_token
```

La respuesta incluye `"access_token": "eyJ..."`. Copia ese valor para los siguientes pasos.

---

## 3. Probar endpoints protegidos

Sustituye `TU_TOKEN_AQUI` por el `access_token` obtenido.

### Endpoint público (sin token)

```bash
curl http://localhost:8082/clases/public/status
curl http://localhost:8081/miembros/public/status
curl http://localhost:8083/entrenadores/public/status
curl http://localhost:8084/equipos/public/status
```

### Endpoints protegidos (con token)

**Clases (puerto 8082):**
```bash
curl -X GET "http://localhost:8082/clases" -H "Authorization: Bearer TU_TOKEN_AQUI"
```

**Miembros (puerto 8081):**
```bash
curl -X GET "http://localhost:8081/miembros" -H "Authorization: Bearer TU_TOKEN_AQUI"
```

**Entrenadores (puerto 8083):**
```bash
curl -X GET "http://localhost:8083/entrenadores" -H "Authorization: Bearer TU_TOKEN_AQUI"
```

**Equipos (puerto 8084):**
```bash
curl -X GET "http://localhost:8084/equipos" -H "Authorization: Bearer TU_TOKEN_AQUI"
```

**Qué esperar:**

| Situación | Código HTTP | Respuesta típica |
|-----------|-------------|------------------|
| **Token válido** (correcto, no expirado) | **200 OK** | Cuerpo con los datos del recurso (lista de clases, miembros, etc.), por ejemplo `[]` o `[{ ... }, ...]`. |
| **Sin token** (no envías header `Authorization`) | **401 Unauthorized** | Cuerpo en JSON con mensaje de error de Spring Security (ej. "Unauthorized"). |
| **Token inválido** (mal formado, expirado o firmado por otro issuer) | **401 Unauthorized** | Igual que sin token: no autenticado. |
| **Token válido pero sin el rol necesario** (ej. MEMBER en un endpoint solo TRAINER) | **403 Forbidden** | Cuerpo indicando acceso denegado. |

---

## 4. Puertos por servicio

| Servicio   | Puerto |
|-----------|--------|
| Eureka    | 8761   |
| Miembros  | 8081   |
| Clases    | 8082   |
| Entrenadores | 8083 |
| Equipos   | 8084   |

---

## 5. Sin token (debe devolver 401)

```bash
curl -X GET "http://localhost:8082/clases"
```

Respuesta esperada: `401 Unauthorized` (o similar).
