@echo off
rem Construyendo Valores - tarea diaria: crea el video del dia y lo sube programado a las 9:00
cd /d "C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio" || exit /b 1
if not exist "C:\Users\Davia\ConstruyendoValores" mkdir "C:\Users\Davia\ConstruyendoValores"
"C:\Program Files\Common Files\Oracle\Java\javapath\java.exe" -jar target\construyendo-valores-studio-1.0.0.jar --diario "C:\Users\Davia\ConstruyendoValores\videos_diarios" >> "C:\Users\Davia\ConstruyendoValores\diario.log" 2>&1