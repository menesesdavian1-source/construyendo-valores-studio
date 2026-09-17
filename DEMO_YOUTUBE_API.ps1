# DEMO YOUTUBE API — CASO DE USO COMPLETO (para el audit)
# Usa la YouTube Data API v3 con OAuth 2.0. Cada paso va numerado y logueado,
# ideal para grabar con un  grabador de pantalla y adjuntar el resultado en el
# correo de soporte de YouTube.

$ErrorActionPreference = "Stop"
$JAR   = Join-Path $PSScriptRoot "target\construyendo-valores-studio-1.0.0.jar"
$VIDEO = Join-Path $PSScriptRoot "videos_piloto\piloto_torrecuente_storyboard.mp4"
if (-not (Test-Path $JAR))  { Write-Error "No esta el JAR. Compilalo con mvn package." }
if (-not (Test-Path $VIDEO)) { Write-Error "No esta el video de prueba: $VIDEO" }

function Run-Step([string]$Step, [string[]]$ArgsList) {
    Write-Host ""
    Write-Host "===================================================================" -ForegroundColor Cyan
    Write-Host "  $Step" -ForegroundColor Cyan
    Write-Host "===================================================================" -ForegroundColor Cyan
    & "C:\Program Files\Common Files\Oracle\Java\javapath\java.exe" -jar $JAR @ArgsList
    if ($LASTEXITCODE -ne 0) { Write-Error "Fallo en $Step" }
}

Run-Step "PASO 1 - Verificar conexion y permiso de la API"  @("--youtube-test")
Run-Step "PASO 2 - Obtener datos del canal autenticado"     @("--youtube-canal")
Run-Step "PASO 3 - Subir video (resumable, privado)"         @("--youtube-preview", $VIDEO, "Construyendo Valores - Demo subida API (caso de uso audit)", "Caso de uso completo para el audit de cuota: OAuth 2.0, subida resumable mp4 y confirmacion en la cuenta.")
Run-Step "PASO 4 - Listar ultimos videos del canal"          @("--youtube-ultimos")

Write-Host ""
Write-Host "FINALIZADO. Revisa en YouTube Studio > Contenido que el video de prueba este en Privado." -ForegroundColor Green