@echo off
rem Construyendo Valores - tarea diaria: crea el video del dia y lo sube programado a las 9:00
cd /d "C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio" || exit /b 1
if not exist "C:\Users\Davia\ConstruyendoValores" mkdir "C:\Users\Davia\ConstruyendoValores"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"
echo === %DATE% %TIME% inicio tarea diaria === >> "C:\Users\Davia\ConstruyendoValores\diario.log"
"%JAVA%" -jar target\construyendo-valores-studio-1.0.0.jar --diario "C:\Users\Davia\ConstruyendoValores\videos_diarios" >> "C:\Users\Davia\ConstruyendoValores\diario.log" 2>&1
echo === %DATE% %TIME% fin (codigo %ERRORLEVEL%) === >> "C:\Users\Davia\ConstruyendoValores\diario.log"
exit /b %ERRORLEVEL%
