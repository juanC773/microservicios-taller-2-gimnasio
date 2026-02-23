# Health check: curl a /actuator/health de cada microservicio.
# Ejecutar con los 4 servicios levantados.

$services = @(
    @{ Name = "Miembros";     Port = 8081 },
    @{ Name = "Clases";       Port = 8082 },
    @{ Name = "Entrenadores"; Port = 8083 },
    @{ Name = "Equipos";      Port = 8084 }
)

Write-Host "`n--- Health check microservicios ---`n" -ForegroundColor Cyan

foreach ($s in $services) {
    $url = "http://localhost:$($s.Port)/actuator/health"
    try {
        $r = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
        $body = $r.Content | ConvertFrom-Json
        if ($r.StatusCode -eq 200 -and $body.status -eq "UP") {
            Write-Host "[ OK ] $($s.Name) (puerto $($s.Port))" -ForegroundColor Green
        } else {
            Write-Host "[ ?? ] $($s.Name) (puerto $($s.Port)) - status: $($r.StatusCode)" -ForegroundColor Yellow
        }
    } catch {
        Write-Host "[FAIL] $($s.Name) (puerto $($s.Port)) - no responde" -ForegroundColor Red
    }
}

Write-Host "`n--- Fin ---`n" -ForegroundColor Cyan
