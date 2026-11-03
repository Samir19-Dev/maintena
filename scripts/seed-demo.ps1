#requires -Version 5.1
<#
.SYNOPSIS
	Carga datos de demostración masivos en Maintena usando la API REST.

.DESCRIPTION
	Crea categorías, ubicaciones, activos, usuarios, órdenes, reportes,
	comentarios y adjuntos. El script es idempotente: los registros que ya
	existen (misma clave única) se omiten, por lo que se puede ejecutar
	varias veces sin duplicar información.

	Los datos de negocio se crean a través de la API para respetar las
	validaciones, la generación de códigos de orden, la auditoría, las
	notificaciones y la conversión de reportes en órdenes.

.PARAMETER BaseUrl
	URL base de la API. Por defecto http://localhost:8080/api/v1

.PARAMETER AdminEmail
	Email del administrador usado para autenticar.

.PARAMETER AdminPassword
	Contraseña del administrador.

.PARAMETER DemoPassword
	Contraseña asignada a los usuarios demo nuevos.

.EXAMPLE
	powershell -ExecutionPolicy Bypass -File .\scripts\seed-demo.ps1

.EXAMPLE
	.\scripts\seed-demo.ps1 -BaseUrl http://localhost:8080/api/v1
#>
[CmdletBinding()]
param(
	[string]$BaseUrl = 'http://localhost:8080/api/v1',
	[string]$AdminEmail = 'admin@maintena.com',
	[string]$AdminPassword = 'Admin123*',
	[string]$DemoPassword = 'Demo123*'
)

$ErrorActionPreference = 'Stop'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
Add-Type -AssemblyName System.Net.Http

$script:Token = $null
$script:CreatedCount = 0
$script:SkippedCount = 0
$script:FailedCount = 0
$script:Failures = New-Object System.Collections.Generic.List[string]

# ---------------------------------------------------------------------------
# Utilidades
# ---------------------------------------------------------------------------

function Write-Step {
	param([string]$Message)
	Write-Host "`n== $Message ==" -ForegroundColor Cyan
}

function Write-Ok {
	param([string]$Message)
	Write-Host "  + $Message" -ForegroundColor Green
}

function Write-Skip {
	param([string]$Message)
	Write-Host "  = $Message" -ForegroundColor DarkGray
}

function Write-Warn2 {
	param([string]$Message)
	Write-Host "  ! $Message" -ForegroundColor Yellow
}

function ConvertTo-JsonBody {
	param($Object)
	# La coma evita que PowerShell enumere el arreglo de bytes al retornarlo.
	return ,([Text.Encoding]::UTF8.GetBytes(($Object | ConvertTo-Json -Depth 10)))
}

function Get-FailureDetail {
	param($ErrorRecord)
	$detail = $ErrorRecord.Exception.Message
	if ($ErrorRecord.ErrorDetails -and $ErrorRecord.ErrorDetails.Message) {
		$detail = "$detail | $($ErrorRecord.ErrorDetails.Message)"
	}
	return $detail
}

function Invoke-Api {
	param(
		[string]$Method,
		[string]$Path,
		$Body,
		[string]$Token = $script:Token
	)

	# Se usa HttpClient con UTF-8 explícito porque Invoke-RestMethod de
	# Windows PowerShell 5.1 decodifica las respuestas JSON como ISO-8859-1
	# y corrompe los acentos.
	$client = [System.Net.Http.HttpClient]::new()
	$request = $null
	try {
		$request = [System.Net.Http.HttpRequestMessage]::new(
			[System.Net.Http.HttpMethod]::new($Method.ToUpperInvariant()),
			"$BaseUrl$Path"
		)
		if ($Token) {
			$request.Headers.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $Token)
		}
		if ($null -ne $Body) {
			$json = $Body | ConvertTo-Json -Depth 10
			$request.Content = [System.Net.Http.StringContent]::new($json, [Text.Encoding]::UTF8, 'application/json')
		}

		$response = $client.SendAsync($request).GetAwaiter().GetResult()
		$bytes = $response.Content.ReadAsByteArrayAsync().GetAwaiter().GetResult()
		$text = [Text.Encoding]::UTF8.GetString($bytes)

		if (-not $response.IsSuccessStatusCode) {
			throw "HTTP $([int]$response.StatusCode): $text"
		}
		if ([string]::IsNullOrWhiteSpace($text)) { return $null }
		return ($text | ConvertFrom-Json)
	} finally {
		if ($request) { $request.Dispose() }
		$client.Dispose()
	}
}

function Get-List {
	param([string]$Path, [string]$Token = $script:Token)
	$result = Invoke-Api -Method Get -Path $Path -Token $Token
	if ($null -eq $result) { return @() }
	return @($result)
}

function Build-Cache {
	param([string]$Path, [string]$KeyProperty)
	$cache = @{}
	foreach ($item in (Get-List $Path)) {
		$cache[[string]$item.$KeyProperty] = $item
	}
	return $cache
}

function New-Record {
	param([string]$Path, $Body, [string]$Label)
	try {
		$result = Invoke-Api -Method Post -Path $Path -Body $Body
		$script:CreatedCount = [int]$script:CreatedCount + 1
		Write-Ok "Creado: $Label"
		return $result
	} catch {
		$script:FailedCount = [int]$script:FailedCount + 1
		$detail = Get-FailureDetail $_
		$script:Failures.Add("$Label -> $detail")
		Write-Warn2 "Error creando $Label -> $detail"
		return $null
	}
}

function Update-Status {
	param([long]$OrderId, [string]$Estado, [string]$Observaciones, [string]$Label)
	try {
		$body = @{ estado = $Estado }
		if ($Observaciones) { $body.observaciones = $Observaciones }
		Invoke-Api -Method Patch -Path "/maintenance/orders/$OrderId/status" -Body $body | Out-Null
		Write-Ok "$Label -> $Estado"
		return $true
	} catch {
		$script:FailedCount = [int]$script:FailedCount + 1
		$detail = Get-FailureDetail $_
		$script:Failures.Add("$Label estado $Estado -> $detail")
		Write-Warn2 "Error cambiando estado de $Label -> $detail"
		return $false
	}
}

function Login {
	param([string]$Email, [string]$Password)
	$body = @{ email = $Email; password = $Password }
	$response = Invoke-Api -Method Post -Path '/auth/login' -Body $body -Token $null
	return $response.accessToken
}

# ---------------------------------------------------------------------------
# Definición de datos
# ---------------------------------------------------------------------------

$categorySpecs = @(
	@{ Nombre = 'Redes y conectividad'; Descripcion = 'Switches, routers, access points y cableado estructurado' },
	@{ Nombre = 'Servidores y almacenamiento'; Descripcion = 'Servidores, NAS y unidades de respaldo' },
	@{ Nombre = 'Impresoras y periféricos'; Descripcion = 'Impresoras, escáneres y equipos de salida' },
	@{ Nombre = 'Telefonía IP'; Descripcion = 'Teléfonos IP y centrales telefónicas' },
	@{ Nombre = 'Instalaciones eléctricas'; Descripcion = 'Tableros, acometidas y puntos eléctricos' },
	@{ Nombre = 'Climatización'; Descripcion = 'Aires acondicionados y ventilación' },
	@{ Nombre = 'Mobiliario de oficina'; Descripcion = 'Sillas, escritorios y mobiliario general' },
	@{ Nombre = 'Equipos de seguridad'; Descripcion = 'Detectores, extintores y señalización' },
	@{ Nombre = 'Herramientas manuales'; Descripcion = 'Herramientas y equipos de taller' },
	@{ Nombre = 'Vehículos de servicio'; Descripcion = 'Camionetas y motocicletas de operación' },
	@{ Nombre = 'Planta eléctrica'; Descripcion = 'Plantas y generadores de respaldo' },
	@{ Nombre = 'Circuito cerrado de televisión'; Descripcion = 'Cámaras, NVR y monitoreo' }
)

$locationSpecs = @(
	@{ Nombre = 'Sede Norte - Piso 1'; Edificio = 'Torre Empresarial Norte'; Piso = 'Piso 1'; Descripcion = 'Recepción y áreas comerciales' },
	@{ Nombre = 'Sede Norte - Piso 2'; Edificio = 'Torre Empresarial Norte'; Piso = 'Piso 2'; Descripcion = 'Áreas administrativas' },
	@{ Nombre = 'Sede Norte - Piso 3'; Edificio = 'Torre Empresarial Norte'; Piso = 'Piso 3'; Descripcion = 'Salas de reuniones y capacitación' },
	@{ Nombre = 'Sede Norte - Piso 4'; Edificio = 'Torre Empresarial Norte'; Piso = 'Piso 4'; Descripcion = 'Dirección y oficinas ejecutivas' },
	@{ Nombre = 'Sede Sur - Piso 1'; Edificio = 'Bloque Administrativo Sur'; Piso = 'Piso 1'; Descripcion = 'Tesorería y atención al público' },
	@{ Nombre = 'Sede Sur - Piso 2'; Edificio = 'Bloque Administrativo Sur'; Piso = 'Piso 2'; Descripcion = 'Gestión humana y compras' },
	@{ Nombre = 'Sede Sur - Piso 3'; Edificio = 'Bloque Administrativo Sur'; Piso = 'Piso 3'; Descripcion = 'Tecnología y comunicaciones' },
	@{ Nombre = 'Bodega Central - Piso 1'; Edificio = 'Bloque Logístico'; Piso = 'Piso 1'; Descripcion = 'Almacén de repuestos' },
	@{ Nombre = 'Bodega Central - Piso 2'; Edificio = 'Bloque Logístico'; Piso = 'Piso 2'; Descripcion = 'Archivo y suministros' },
	@{ Nombre = 'Bodega Central - Piso 3'; Edificio = 'Bloque Logístico'; Piso = 'Piso 3'; Descripcion = 'Taller de mantenimiento' },
	@{ Nombre = 'Data Center - Piso 1'; Edificio = 'Bloque Técnico'; Piso = 'Piso 1'; Descripcion = 'Sala de servidores principal' },
	@{ Nombre = 'Data Center - Piso 2'; Edificio = 'Bloque Técnico'; Piso = 'Piso 2'; Descripcion = 'Sala de comunicaciones y respaldo' }
)

$userSpecs = @(
	@{ Nombre = 'Laura'; Apellido = 'Gómez'; Email = 'demo.tecnico1@maintena.com'; Telefono = '3001000001'; Rol = 'TECNICO' },
	@{ Nombre = 'Andrés'; Apellido = 'Ramírez'; Email = 'demo.tecnico2@maintena.com'; Telefono = '3001000002'; Rol = 'TECNICO' },
	@{ Nombre = 'Sofía'; Apellido = 'Herrera'; Email = 'demo.tecnico3@maintena.com'; Telefono = '3001000003'; Rol = 'TECNICO' },
	@{ Nombre = 'Julián'; Apellido = 'Castro'; Email = 'demo.tecnico4@maintena.com'; Telefono = '3001000004'; Rol = 'TECNICO' },
	@{ Nombre = 'Valentina'; Apellido = 'Ríos'; Email = 'demo.tecnico5@maintena.com'; Telefono = '3001000005'; Rol = 'TECNICO' },
	@{ Nombre = 'Camila'; Apellido = 'Torres'; Email = 'demo.empleado1@maintena.com'; Telefono = '3002000001'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Diego'; Apellido = 'Moreno'; Email = 'demo.empleado2@maintena.com'; Telefono = '3002000002'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Paula'; Apellido = 'Jiménez'; Email = 'demo.empleado3@maintena.com'; Telefono = '3002000003'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Ricardo'; Apellido = 'Salazar'; Email = 'demo.empleado4@maintena.com'; Telefono = '3002000004'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Natalia'; Apellido = 'Vargas'; Email = 'demo.empleado5@maintena.com'; Telefono = '3002000005'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Felipe'; Apellido = 'Ortiz'; Email = 'demo.empleado6@maintena.com'; Telefono = '3002000006'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Mariana'; Apellido = 'Cárdenas'; Email = 'demo.empleado7@maintena.com'; Telefono = '3002000007'; Rol = 'EMPLEADO' },
	@{ Nombre = 'Jorge'; Apellido = 'Peña'; Email = 'demo.supervisor@maintena.com'; Telefono = '3003000001'; Rol = 'SUPERVISOR' },
	@{ Nombre = 'Diana'; Apellido = 'Restrepo'; Email = 'demo.admin@maintena.com'; Telefono = '3004000001'; Rol = 'ADMIN' }
)

$assetSpecs = @(
	@{ Nombre = 'Switch Cisco Catalyst 9200'; Marca = 'Cisco'; Modelo = 'C9200-24P'; Serial = 'DEMO-SW-100'; Categoria = 'Redes y conectividad'; Ubicacion = 'Sede Norte - Piso 1' },
	@{ Nombre = 'Router Fortinet FortiGate 60F'; Marca = 'Fortinet'; Modelo = 'FG-60F'; Serial = 'DEMO-RT-101'; Categoria = 'Redes y conectividad'; Ubicacion = 'Sede Norte - Piso 2' },
	@{ Nombre = 'Access Point Ubiquiti U6 Pro'; Marca = 'Ubiquiti'; Modelo = 'U6-Pro'; Serial = 'DEMO-AP-102'; Categoria = 'Redes y conectividad'; Ubicacion = 'Sede Norte - Piso 3' },
	@{ Nombre = 'Servidor Dell PowerEdge R750'; Marca = 'Dell'; Modelo = 'R750'; Serial = 'DEMO-SRV-103'; Categoria = 'Servidores y almacenamiento'; Ubicacion = 'Data Center - Piso 1' },
	@{ Nombre = 'NAS Synology RS1221+'; Marca = 'Synology'; Modelo = 'RS1221+'; Serial = 'DEMO-NAS-104'; Categoria = 'Servidores y almacenamiento'; Ubicacion = 'Data Center - Piso 2' },
	@{ Nombre = 'UPS APC Smart-UPS 3000'; Marca = 'APC'; Modelo = 'SMT3000'; Serial = 'DEMO-UPS-105'; Categoria = 'Servidores y almacenamiento'; Ubicacion = 'Data Center - Piso 1' },
	@{ Nombre = 'Impresora HP LaserJet M428fdw'; Marca = 'HP'; Modelo = 'M428fdw'; Serial = 'DEMO-PRN-106'; Categoria = 'Impresoras y periféricos'; Ubicacion = 'Sede Norte - Piso 1' },
	@{ Nombre = 'Impresora Epson EcoTank L3250'; Marca = 'Epson'; Modelo = 'L3250'; Serial = 'DEMO-PRN-107'; Categoria = 'Impresoras y periféricos'; Ubicacion = 'Sede Sur - Piso 1' },
	@{ Nombre = 'Escáner Canon imageFORMULA R40'; Marca = 'Canon'; Modelo = 'R40'; Serial = 'DEMO-SCN-108'; Categoria = 'Impresoras y periféricos'; Ubicacion = 'Sede Sur - Piso 2' },
	@{ Nombre = 'Teléfono IP Yealink T33G'; Marca = 'Yealink'; Modelo = 'T33G'; Serial = 'DEMO-TEL-109'; Categoria = 'Telefonía IP'; Ubicacion = 'Sede Norte - Piso 4' },
	@{ Nombre = 'Central telefónica Grandstream UCM6302'; Marca = 'Grandstream'; Modelo = 'UCM6302'; Serial = 'DEMO-PBX-110'; Categoria = 'Telefonía IP'; Ubicacion = 'Sede Norte - Piso 4' },
	@{ Nombre = 'Tablero eléctrico Schneider Acti9'; Marca = 'Schneider'; Modelo = 'Acti9'; Serial = 'DEMO-ELE-111'; Categoria = 'Instalaciones eléctricas'; Ubicacion = 'Sede Sur - Piso 1' },
	@{ Nombre = 'Planta eléctrica Cummins 60 kVA'; Marca = 'Cummins'; Modelo = 'C60D5'; Serial = 'DEMO-GEN-112'; Categoria = 'Planta eléctrica'; Ubicacion = 'Bodega Central - Piso 1' },
	@{ Nombre = 'Aire acondicionado LG Inverter 24000 BTU'; Marca = 'LG'; Modelo = 'VM242C7'; Serial = 'DEMO-HVAC-113'; Categoria = 'Climatización'; Ubicacion = 'Sede Norte - Piso 2' },
	@{ Nombre = 'Aire acondicionado Samsung 18000 BTU'; Marca = 'Samsung'; Modelo = 'AR18TYHZ'; Serial = 'DEMO-HVAC-114'; Categoria = 'Climatización'; Ubicacion = 'Sede Sur - Piso 3' },
	@{ Nombre = 'Silla ergonómica Herman Miller Aeron'; Marca = 'Herman Miller'; Modelo = 'Aeron B'; Serial = 'DEMO-MOB-115'; Categoria = 'Mobiliario de oficina'; Ubicacion = 'Sede Norte - Piso 3' },
	@{ Nombre = 'Escritorio regulable Steelcase Series 1'; Marca = 'Steelcase'; Modelo = 'Series 1'; Serial = 'DEMO-MOB-116'; Categoria = 'Mobiliario de oficina'; Ubicacion = 'Sede Sur - Piso 2' },
	@{ Nombre = 'Cámara Hikvision DS-2CD2143G2'; Marca = 'Hikvision'; Modelo = 'DS-2CD2143G2'; Serial = 'DEMO-CAM-117'; Categoria = 'Circuito cerrado de televisión'; Ubicacion = 'Bodega Central - Piso 2' },
	@{ Nombre = 'NVR Hikvision DS-7608NI'; Marca = 'Hikvision'; Modelo = 'DS-7608NI'; Serial = 'DEMO-NVR-118'; Categoria = 'Circuito cerrado de televisión'; Ubicacion = 'Bodega Central - Piso 3' },
	@{ Nombre = 'Detector de humo Honeywell TC806B'; Marca = 'Honeywell'; Modelo = 'TC806B'; Serial = 'DEMO-SEC-119'; Categoria = 'Equipos de seguridad'; Ubicacion = 'Sede Norte - Piso 1' },
	@{ Nombre = 'Extintor ABC 20 lb'; Marca = 'Kidde'; Modelo = 'Pro 20'; Serial = 'DEMO-SEC-120'; Categoria = 'Equipos de seguridad'; Ubicacion = 'Sede Sur - Piso 1' },
	@{ Nombre = 'Taladro percutor Bosch GSB 20'; Marca = 'Bosch'; Modelo = 'GSB 20-2'; Serial = 'DEMO-HER-121'; Categoria = 'Herramientas manuales'; Ubicacion = 'Bodega Central - Piso 1' },
	@{ Nombre = 'Camioneta Toyota Hilux'; Marca = 'Toyota'; Modelo = 'Hilux 2.8'; Serial = 'DEMO-VEH-122'; Categoria = 'Vehículos de servicio'; Ubicacion = 'Bodega Central - Piso 1' },
	@{ Nombre = 'Motocicleta Yamaha DT 125'; Marca = 'Yamaha'; Modelo = 'DT125'; Serial = 'DEMO-VEH-123'; Categoria = 'Vehículos de servicio'; Ubicacion = 'Bodega Central - Piso 2' }
)

$orderPlan = @(
	@{ Estado = 'PENDIENTE'; Count = 10 },
	@{ Estado = 'ASIGNADA'; Count = 10 },
	@{ Estado = 'EN_PROCESO'; Count = 10 },
	@{ Estado = 'COMPLETADA'; Count = 8 },
	@{ Estado = 'CANCELADA'; Count = 2 }
)

$reportPlan = @(
	@{ Estado = 'PENDIENTE'; Count = 4 },
	@{ Estado = 'CONVERTIDA'; Count = 10 },
	@{ Estado = 'RECHAZADA'; Count = 4 }
)

$failureDescriptions = @(
	'El equipo presenta sobrecalentamiento y se apaga solo',
	'No enciende después de un corte de energía',
	'Ruido anormal y vibración en el motor',
	'Fuga de agua en la unidad de climatización',
	'Pantalla con líneas y parpadeo constante',
	'Cable de alimentación deteriorado',
	'No responde a los comandos y queda bloqueado',
	'Consumo eléctrico elevado en el tablero',
	'Impresora con atascos recurrentes de papel',
	'Cámara sin señal en el monitoreo',
	'Extintor con sello de recarga vencido',
	'Silla con pistón hidráulico defectuoso',
	'Servidor con alerta de disco degradado',
	'UPS emite alarma continua y no mantiene carga',
	'Teléfono IP sin tono de marcado',
	'Switch con puertos deshabilitados',
	'Vehículo con frenos desgastados',
	'Access point con intermitencia de red'
)

$commentTexts = @(
	'Se recibió el equipo y se inició la revisión inicial.',
	'Se solicitó repuesto al área de compras.',
	'Se realizó limpieza interna y ajuste de conexiones.',
	'Pendiente prueba de funcionamiento con el usuario.',
	'Se documentó la falla en el formato de mantenimiento.',
	'Se reemplazó el componente afectado.',
	'El equipo queda operativo y se entrega al área.',
	'Se recomienda programar mantenimiento preventivo.',
	'Se verificó el voltaje y la puesta a tierra.',
	'Se actualizó el firmware del dispositivo.'
)

# PNG 1x1 y PDF mínimo válidos para las evidencias.
$pngBase64 = 'iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=='
$pdfText = @"
%PDF-1.4
1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 300 200]>>endobj
trailer<</Root 1 0 R>>
%%EOF
"@
$pngBytes = [Convert]::FromBase64String($pngBase64)
$pdfBytes = [Text.Encoding]::ASCII.GetBytes($pdfText)

# ---------------------------------------------------------------------------
# Inicio
# ---------------------------------------------------------------------------

Write-Host ''
Write-Host 'Maintena - carga de datos de demostración' -ForegroundColor White
Write-Host "API: $BaseUrl" -ForegroundColor DarkGray

Write-Step 'Autenticando como administrador'
$script:Token = Login -Email $AdminEmail -Password $AdminPassword
if (-not $script:Token) {
	throw "No fue posible autenticar a $AdminEmail"
}
Write-Ok "Sesión iniciada como $AdminEmail"

$roles = @{}
foreach ($role in (Get-List '/roles')) { $roles[$role.nombre] = $role }

# ---------------------------------------------------------------------------
# Categorías
# ---------------------------------------------------------------------------

Write-Step 'Categorías'
$categoryCache = Build-Cache -Path '/categories' -KeyProperty 'nombre'
foreach ($spec in $categorySpecs) {
	if ($categoryCache.ContainsKey($spec.Nombre)) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Skip "Categoría existente: $($spec.Nombre)"
		continue
	}
	$created = New-Record -Path '/categories' -Label "categoría '$($spec.Nombre)'" -Body @{
		nombre      = $spec.Nombre
		descripcion = $spec.Descripcion
		activo      = $true
	}
	if ($created) { $categoryCache[$spec.Nombre] = $created }
}

# ---------------------------------------------------------------------------
# Ubicaciones
# ---------------------------------------------------------------------------

Write-Step 'Ubicaciones'
$locationCache = Build-Cache -Path '/locations' -KeyProperty 'nombre'
foreach ($spec in $locationSpecs) {
	if ($locationCache.ContainsKey($spec.Nombre)) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Skip "Ubicación existente: $($spec.Nombre)"
		continue
	}
	$created = New-Record -Path '/locations' -Label "ubicación '$($spec.Nombre)'" -Body @{
		nombre      = $spec.Nombre
		edificio    = $spec.Edificio
		piso        = $spec.Piso
		descripcion = $spec.Descripcion
		activo      = $true
	}
	if ($created) { $locationCache[$spec.Nombre] = $created }
}

# ---------------------------------------------------------------------------
# Usuarios
# ---------------------------------------------------------------------------

Write-Step 'Usuarios demo'
$userCache = Build-Cache -Path '/users' -KeyProperty 'email'
foreach ($spec in $userSpecs) {
	if ($userCache.ContainsKey($spec.Email)) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Skip "Usuario existente: $($spec.Email)"
		continue
	}
	$role = $roles[$spec.Rol]
	if (-not $role) {
		Write-Warn2 "Rol '$($spec.Rol)' no encontrado; se omite $($spec.Email)"
		continue
	}
	$created = New-Record -Path '/users' -Label "usuario $($spec.Email) [$($spec.Rol)]" -Body @{
		nombre   = $spec.Nombre
		apellido = $spec.Apellido
		email    = $spec.Email
		telefono = $spec.Telefono
		password = $DemoPassword
		rolId    = $role.id
		activo   = $true
	}
	if ($created) { $userCache[$spec.Email] = $created }
}

# ---------------------------------------------------------------------------
# Activos
# ---------------------------------------------------------------------------

Write-Step 'Activos'
$assetCache = Build-Cache -Path '/assets' -KeyProperty 'codigo'
$assetSequence = 100
foreach ($spec in $assetSpecs) {
	$codigo = 'ACT-{0}' -f $assetSequence
	$assetSequence++
	if ($assetCache.ContainsKey($codigo)) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Skip "Activo existente: $codigo"
		continue
	}
	$category = $categoryCache[$spec.Categoria]
	$location = $locationCache[$spec.Ubicacion]
	if (-not $category -or -not $location) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Warn2 "Faltan categoría/ubicación para $codigo; se omite"
		continue
	}
	$created = New-Record -Path '/assets' -Label "activo $codigo - $($spec.Nombre)" -Body @{
		codigo           = $codigo
		nombre           = $spec.Nombre
		descripcion      = "Activo de demostración para pruebas de mantenimiento"
		marca            = $spec.Marca
		modelo           = $spec.Modelo
		serial           = $spec.Serial
		fechaAdquisicion = (Get-Date).AddYears(-2).ToString('yyyy-MM-dd')
		categoriaId      = $category.id
		ubicacionId      = $location.id
		activo           = $true
	}
	if ($created) { $assetCache[$codigo] = $created }
}

$demoAssets = @($assetSpecs | ForEach-Object -Begin { $i = 100 } -Process {
	$codigo = 'ACT-{0}' -f $i
	$i++
	if ($assetCache.ContainsKey($codigo)) { $assetCache[$codigo] }
})

if ($demoAssets.Count -eq 0) {
	throw 'No hay activos disponibles para crear órdenes'
}

# ---------------------------------------------------------------------------
# Técnicos y empleados demo
# ---------------------------------------------------------------------------

$techSpecs = @($userSpecs | Where-Object { $_.Rol -eq 'TECNICO' })
$employeeSpecs = @($userSpecs | Where-Object { $_.Rol -eq 'EMPLEADO' })

$technicians = @()
foreach ($spec in $techSpecs) {
	if ($userCache.ContainsKey($spec.Email)) { $technicians += $userCache[$spec.Email] }
}
if ($technicians.Count -eq 0) {
	throw 'No hay técnicos demo disponibles para asignar órdenes'
}

# ---------------------------------------------------------------------------
# Órdenes
# ---------------------------------------------------------------------------

Write-Step 'Órdenes de mantenimiento'
$orderCache = Build-Cache -Path '/maintenance/orders' -KeyProperty 'titulo'
$orderIndex = 0

foreach ($group in $orderPlan) {
	for ($i = 0; $i -lt $group.Count; $i++) {
		$orderIndex++
		$asset = $demoAssets[($orderIndex - 1) % $demoAssets.Count]
		$priority = @('BAJA', 'MEDIA', 'ALTA', 'URGENTE')[($orderIndex - 1) % 4]
		$tipo = if (($orderIndex % 3) -eq 0) { 'CORRECTIVO' } else { 'PREVENTIVO' }
		$titulo = '[Demo] #{0:D2} {1} - {2}' -f $orderIndex, $(if ($tipo -eq 'PREVENTIVO') { 'Mantenimiento preventivo' } else { 'Mantenimiento correctivo' }), $asset.codigo

		if ($orderCache.ContainsKey($titulo)) {
			$script:SkippedCount = [int]$script:SkippedCount + 1
			Write-Skip "Orden existente: $titulo"
			continue
		}

		$technician = $null
		if ($group.Estado -in @('ASIGNADA', 'EN_PROCESO', 'COMPLETADA')) {
			$technician = $technicians[($orderIndex - 1) % $technicians.Count]
		}

		$body = @{
			titulo          = $titulo
			descripcion     = "Orden de demostración #$orderIndex para pruebas de la plataforma."
			tipo            = $tipo
			prioridad       = $priority
			activoId        = $asset.id
			fechaProgramada = (Get-Date).AddDays(($orderIndex % 20) - 5).ToString('yyyy-MM-dd')
			observaciones   = 'Registro generado por el script de datos de prueba.'
		}
		if ($technician) { $body.tecnicoId = $technician.id }
		if ($group.Estado -eq 'COMPLETADA') { $body.costo = 150000 + ($orderIndex * 1000) }

		$order = New-Record -Path '/maintenance/orders' -Label "orden $titulo" -Body $body
		if (-not $order) { continue }
		$orderCache[$titulo] = $order

		switch ($group.Estado) {
			'ASIGNADA' { }
			'EN_PROCESO' {
				Update-Status -OrderId $order.id -Estado 'EN_PROCESO' -Observaciones 'Trabajo iniciado por el técnico asignado.' -Label $order.codigo | Out-Null
			}
			'COMPLETADA' {
				Update-Status -OrderId $order.id -Estado 'EN_PROCESO' -Observaciones 'Trabajo iniciado por el técnico asignado.' -Label $order.codigo | Out-Null
				Update-Status -OrderId $order.id -Estado 'COMPLETADA' -Observaciones 'Mantenimiento finalizado y verificado con el usuario.' -Label $order.codigo | Out-Null
			}
			'CANCELADA' {
				Update-Status -OrderId $order.id -Estado 'CANCELADA' -Observaciones 'Orden cancelada por duplicidad en la solicitud.' -Label $order.codigo | Out-Null
			}
			'PENDIENTE' { }
		}
	}
}

# ---------------------------------------------------------------------------
# Reportes de falla
# ---------------------------------------------------------------------------

Write-Step 'Reportes de falla'

# Inicia sesión como empleado para que el solicitante del reporte sea real.
$employeeTokens = @{}
foreach ($spec in $employeeSpecs) {
	try {
		$employeeTokens[$spec.Email] = Login -Email $spec.Email -Password $DemoPassword
	} catch {
		Write-Warn2 "No se pudo iniciar sesión como $($spec.Email); se usará el administrador"
		$employeeTokens[$spec.Email] = $script:Token
	}
}

$reportCache = @{}
foreach ($report in (Get-List '/maintenance/reports')) {
	$reportCache[[string]$report.descripcion] = $report
}

$reportIndex = 0
foreach ($group in $reportPlan) {
	for ($i = 0; $i -lt $group.Count; $i++) {
		$reportIndex++
		$asset = $demoAssets[($reportIndex - 1) % $demoAssets.Count]
		$employeeSpec = $employeeSpecs[($reportIndex - 1) % $employeeSpecs.Count]
		$employeeToken = $employeeTokens[$employeeSpec.Email]
		$descripcion = '[Demo] Reporte #{0:D2} - {1}' -f $reportIndex, $failureDescriptions[($reportIndex - 1) % $failureDescriptions.Count]
		$priority = @('BAJA', 'MEDIA', 'ALTA', 'URGENTE')[($reportIndex - 1) % 4]

		if ($reportCache.ContainsKey($descripcion)) {
			$script:SkippedCount = [int]$script:SkippedCount + 1
			Write-Skip "Reporte existente: #$reportIndex"
			continue
		}

		try {
			$report = Invoke-Api -Method Post -Path '/maintenance/reports' -Token $employeeToken -Body @{
				descripcion = $descripcion
				activoId    = $asset.id
				prioridad   = $priority
			}
			$script:CreatedCount = [int]$script:CreatedCount + 1
			Write-Ok "Reporte #$reportIndex creado por $($employeeSpec.Email)"
		} catch {
			$script:FailedCount = [int]$script:FailedCount + 1
			$detail = Get-FailureDetail $_
			$script:Failures.Add("reporte #$reportIndex -> $detail")
			Write-Warn2 "Error creando reporte #$reportIndex -> $detail"
			continue
		}
		$reportCache[$descripcion] = $report

		if ($group.Estado -eq 'CONVERTIDA') {
			try {
				Invoke-Api -Method Patch -Path "/maintenance/reports/$($report.id)/approve" | Out-Null
				Write-Ok "Reporte #$reportIndex aprobado y convertido en orden"
			} catch {
				$script:FailedCount = [int]$script:FailedCount + 1
				$detail = Get-FailureDetail $_
				$script:Failures.Add("aprobar reporte #$reportIndex -> $detail")
				Write-Warn2 "Error aprobando reporte #$reportIndex -> $detail"
			}
		}
		elseif ($group.Estado -eq 'RECHAZADA') {
			try {
				Invoke-Api -Method Patch -Path "/maintenance/reports/$($report.id)/reject" | Out-Null
				Write-Ok "Reporte #$reportIndex rechazado"
			} catch {
				$script:FailedCount = [int]$script:FailedCount + 1
				$detail = Get-FailureDetail $_
				$script:Failures.Add("rechazar reporte #$reportIndex -> $detail")
				Write-Warn2 "Error rechazando reporte #$reportIndex -> $detail"
			}
		}
	}
}

# ---------------------------------------------------------------------------
# Comentarios
# ---------------------------------------------------------------------------

Write-Step 'Comentarios en órdenes'
$allOrders = @(Get-List '/maintenance/orders')
$commentTargets = @($allOrders | Where-Object { $_.estado -in @('ASIGNADA', 'EN_PROCESO', 'COMPLETADA') })
if ($commentTargets.Count -lt 10) {
	$commentTargets += @($allOrders | Where-Object { $_.estado -eq 'PENDIENTE' })
}
$commentTargets = @($commentTargets | Select-Object -First 15)

$commentIndex = 0
foreach ($order in $commentTargets) {
	$existing = @(Get-List "/maintenance/orders/$($order.id)/comments")
	$existingTexts = @{}
	foreach ($c in $existing) { $existingTexts[[string]$c.contenido] = $true }

	for ($j = 0; $j -lt 2; $j++) {
		$commentIndex++
		$texto = '[Demo] {0} ({1:D2})' -f $commentTexts[($commentIndex - 1) % $commentTexts.Count], $commentIndex
		if ($existingTexts.ContainsKey($texto)) {
			$script:SkippedCount = [int]$script:SkippedCount + 1
			Write-Skip "Comentario existente en $($order.codigo)"
			continue
		}
		$created = New-Record -Path "/maintenance/orders/$($order.id)/comments" -Label "comentario en $($order.codigo)" -Body @{
			contenido = $texto
		}
		if ($created) { $existingTexts[$texto] = $true }
	}
}

# ---------------------------------------------------------------------------
# Adjuntos
# ---------------------------------------------------------------------------

Write-Step 'Adjuntos (evidencias)'
$attachmentTargets = @($commentTargets | Select-Object -First 14)
$attachmentIndex = 0

function Send-Attachment {
	param(
		[long]$OrderId,
		[string]$FileName,
		[byte[]]$Bytes,
		[string]$ContentType,
		[string]$Label
	)
	$client = [System.Net.Http.HttpClient]::new()
	$multipart = $null
	try {
		$client.DefaultRequestHeaders.Authorization = [System.Net.Http.Headers.AuthenticationHeaderValue]::new('Bearer', $script:Token)
		$multipart = [System.Net.Http.MultipartFormDataContent]::new()
		$fileContent = [System.Net.Http.ByteArrayContent]::new($Bytes)
		$fileContent.Headers.ContentType = [System.Net.Http.Headers.MediaTypeHeaderValue]::Parse($ContentType)
		$multipart.Add($fileContent, 'file', $FileName)
		$response = $client.PostAsync("$BaseUrl/maintenance/orders/$OrderId/attachments", $multipart).GetAwaiter().GetResult()
		$raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
		if (-not $response.IsSuccessStatusCode) {
			throw "HTTP $([int]$response.StatusCode): $raw"
		}
		$script:CreatedCount = [int]$script:CreatedCount + 1
		Write-Ok "Adjunto $FileName en $Label"
		return $true
	} catch {
		$script:FailedCount = [int]$script:FailedCount + 1
		$detail = Get-FailureDetail $_
		$script:Failures.Add("adjunto $FileName en $Label -> $detail")
		Write-Warn2 "Error adjuntando $FileName en $Label -> $detail"
		return $false
	} finally {
		if ($multipart) { $multipart.Dispose() }
		$client.Dispose()
	}
}

foreach ($order in $attachmentTargets) {
	$attachmentIndex++
	$existing = @(Get-List "/maintenance/orders/$($order.id)/attachments")
	$existingNames = @{}
	foreach ($a in $existing) { $existingNames[[string]$a.nombreOriginal] = $true }

	if (($attachmentIndex % 2) -eq 1) {
		$fileName = 'demo-evidencia-{0:D2}.png' -f $attachmentIndex
		$bytes = $pngBytes
		$contentType = 'image/png'
	} else {
		$fileName = 'demo-informe-{0:D2}.pdf' -f $attachmentIndex
		$bytes = $pdfBytes
		$contentType = 'application/pdf'
	}

	if ($existingNames.ContainsKey($fileName)) {
		$script:SkippedCount = [int]$script:SkippedCount + 1
		Write-Skip "Adjunto existente $fileName en $($order.codigo)"
		continue
	}

	Send-Attachment -OrderId $order.id -FileName $fileName -Bytes $bytes -ContentType $contentType -Label $order.codigo | Out-Null
}

# ---------------------------------------------------------------------------
# Resumen
# ---------------------------------------------------------------------------

Write-Step 'Resumen final'

$finalOrders = @(Get-List '/maintenance/orders')
$commentTotal = 0
$attachmentTotal = 0
foreach ($order in $finalOrders) {
	$commentTotal += @(Get-List "/maintenance/orders/$($order.id)/comments").Count
	$attachmentTotal += @(Get-List "/maintenance/orders/$($order.id)/attachments").Count
}

$summary = [ordered]@{
	'Categorías'  = @(Get-List '/categories').Count
	'Ubicaciones' = @(Get-List '/locations').Count
	'Activos'     = @(Get-List '/assets').Count
	'Usuarios'    = @(Get-List '/users').Count
	'Órdenes'     = $finalOrders.Count
	'Reportes'    = @(Get-List '/maintenance/reports').Count
	'Comentarios' = $commentTotal
	'Adjuntos'    = $attachmentTotal
}

Write-Host ''
foreach ($key in $summary.Keys) {
	Write-Host ("  {0,-14} {1,5}" -f $key, $summary[$key]) -ForegroundColor White
}

Write-Host ''
Write-Host ("  Creados: {0}   Omitidos: {1}   Fallidos: {2}" -f $script:CreatedCount, $script:SkippedCount, $script:FailedCount) -ForegroundColor White

if ($script:FailedCount -gt 0) {
	Write-Host ''
	Write-Host '  Detalle de fallos:' -ForegroundColor Yellow
	foreach ($failure in $script:Failures) {
		Write-Host "   - $failure" -ForegroundColor Yellow
	}
}

Write-Host ''
Write-Host '  Usuarios demo (contraseña común):' -ForegroundColor Cyan
Write-Host "   Técnicos:    demo.tecnico1..5@maintena.com / $DemoPassword"
Write-Host "   Empleados:   demo.empleado1..7@maintena.com / $DemoPassword"
Write-Host "   Supervisor:  demo.supervisor@maintena.com / $DemoPassword"
Write-Host "   Admin extra: demo.admin@maintena.com / $DemoPassword"
Write-Host ''

if ($script:FailedCount -gt 0) {
	exit 1
}
exit 0
