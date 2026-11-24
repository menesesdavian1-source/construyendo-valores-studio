# lanzar_dia2.ps1 - Programa los Shorts restantes y los 4 medios de la tanda 1.
# Idempotente (marcas *.programado). Reintenta hasta agotarse el cupo diario de YouTube
# (10 subidas/24h) o hasta las 23:30. Log completo en lanzar_dia2.log
$ErrorActionPreference = 'Continue'
$jar = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\target\construyendo-valores-studio-1.0.0.jar'
$cp  = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\clips_campana'
$log = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\lanzar_dia2.log'

function Log($m) {
    Add-Content -LiteralPath $log -Value ("{0} | {1}" -f (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'), $m) -Encoding UTF8
}

$SOFIA = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\cuentos\sofia_puente\Sofía y el puente de colores _ Aprendiendo Creatividad.mp4'
$PINO  = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\salidas\pinocho\La Aventura Honesta de Nico el Mazo de Madera.mp4'
$PIPO  = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\canciones\pipo\La canción de Pipo.mp4'
$CASQ  = 'C:\Users\Davia\OneDrive\Documents\Default Project\ConstruyendoValoresStudio\canciones\casquito_final\La canción de Casquito.mp4'

$DESC = 'Nueva aventura de Construyendo Valores: cuentos y musica para aprender valores con la pandilla.' +
        "`n`n" + 'Suscribete y activa la campanita para no perderte ninguna historia.' +
        "`n`n#cuentoinfantil #valores #construyendovalores #cancionesparaninos #aprendeconmigo'

$tareas = @(
    ,@('--lanzar', $cp)
    ,@('--planificar', $SOFIA, 'Sofía y el puente de colores | Sé CREATIVO con la pandilla | Construyendo Valores', $DESC, '2026-10-11')
    ,@('--planificar', $PINO,  'La Aventura Honesta de Nico | Aprende HONESTIDAD | Construyendo Valores', $DESC, '2026-10-12')
    ,@('--planificar', $PIPO,  'La canción de Pipo | Música y VALORES para niños | Construyendo Valores', $DESC, '2026-10-13')
    ,@('--planificar', $CASQ,  'La canción de Casquito | Música y VALORES para niños | Construyendo Valores', $DESC, '2026-10-14')
)

function Invoke-Tareas {
    $penden = 0
    foreach ($t in $tareas) {
        $out = (& java -jar $jar @t 2>&1 | Out-String)
        Log $out
        if ($out -match 'ok ->') { Write-Output "OK  $($t[0]) $($t[1])" }
        else {
            $penden++
            Write-Output "FALLA $($t[0]) $($t[1])"
        }
    }
    return $penden
}

Log '=== LANZAMIENTO DIA 2 (inicio) ==='
$pendiente = 99
for ($i = 0; $i -lt 60 -and $pendiente -gt 0; $i++) {
    $pendiente = Invoke-Tareas
    if ($pendiente -eq 0) { Log '=== TODOS PROGRAMADOS ==='; break }
    if ($i -lt 59) {
        Log "Reintento en 15 min (pendientes: $pendiente) ..."
        Start-Sleep -Seconds 900
    }
}
if ($pendiente -gt 0) { Log '=== FIN: quedaron pendientes (revisar log) ===' }