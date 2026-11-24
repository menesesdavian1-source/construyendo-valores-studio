@echo off
rem Construyendo Valores - tarea semanal: renderiza los proximos 7 videos a la cola (domingo 20:00)
rem La tarea diaria de cada manana solo sube el video que ya dejo preparado esta cola.
cd /d "C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio" || exit /b 1
if not exist "C:\Users\Davia\ConstruyendoValores" mkdir "C:\Users\Davia\ConstruyendoValores"
set "JAVA=C:\Program Files\Eclipse Adoptium\jdk-21.0.13.9-hotspot\bin\java.exe"
if not exist "%JAVA%" set "JAVA=java"
echo === %DATE% %TIME% inicio tarea semanal === >> "C:\Users\Davia\ConstruyendoValores\semana.log"
"%JAVA%" -jar target\construyendo-valores-studio-1.0.0.jar --semanario "C:\Users\Davia\ConstruyendoValores\videos_diarios" 7 >> "C:\Users\Davia\ConstruyendoValores\semana.log" 2>&1
rem Compilacion semanal de video largo (une los videos ya subidos esta semana)
"%JAVA%" -jar target\construyendo-valores-studio-1.0.0.jar --compilacion "C:\Users\Davia\ConstruyendoValores\videos_diarios" >> "C:\Users\Davia\ConstruyendoValores\semana.log" 2>&1
echo === %DATE% %TIME% fin (codigo %ERRORLEVEL%) === >> "C:\Users\Davia\ConstruyendoValores\semana.log"
exit /b %ERRORLEVEL%
