# Demo de subida para el audit de YouTube - Construyendo Valores Studio
# Graba la ventana con Win + Alt + R mientras este script se ejecuta.
# No muestra claves: solo el canal, el video y el resultado.

$ErrorActionPreference = "Stop"
$proj = "C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio"
$jar  = Join-Path $proj "target\construyendo-valores-studio-1.0.0.jar"
$java = "C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }
$video = Join-Path $proj "salidas\cigarra\short_cigarra.mp4"

Clear-Host
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host " Construyendo Valores Studio - YouTube Data API v3 demo" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host ""
Write-Host " 1) OAuth 2.0 scope: https://www.googleapis.com/auth/youtube" -ForegroundColor Gray
Write-Host " 2) Endpoint: youtube.videos.insert (resumable upload)" -ForegroundColor Gray
Write-Host " 3) Single-user desktop tool, owner's own channel" -ForegroundColor Gray
Write-Host ""
Start-Sleep -Seconds 3

Write-Host "[STEP A] Checking the authorized channel ..." -ForegroundColor Yellow
& $java -jar $jar --youtube-test
Write-Host ""
Start-Sleep -Seconds 2

Write-Host "[STEP B] Uploading the finished video ..." -ForegroundColor Yellow
Write-Host ""
& $java -jar $jar --youtube-preview $video `
    "Demo subida API - Construyendo Valores Studio" `
    "End-to-end upload demo for the YouTube Data API audit: resumable videos.insert with OAuth 2.0, single authorized channel, video declared Made for Kids." `
    private

Write-Host ""
Write-Host "==========================================================" -ForegroundColor Green
Write-Host " DONE. The video is now on the channel (private)." -ForegroundColor Green
Write-Host " Next: open the https://youtu.be/... link on screen." -ForegroundColor Green
Write-Host "==========================================================" -ForegroundColor Green
