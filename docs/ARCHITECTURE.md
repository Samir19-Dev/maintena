# Arquitectura del backend

## Principio: organización por funcionalidad

Maintena usa **package-by-feature**: cada módulo agrupa todo lo que pertenece a
un mismo dominio y repite internamente las capas técnicas.

```
com.maintena
├── asset          # Activos
├── audit          # Bitácora de auditoría
├── auth           # Login, refresh, perfil y recuperación de contraseña
├── category       # Categorías de activos
├── common         # Constantes, respuestas y utilidades compartidas
├── config         # Configuración general
├── dashboard      # KPIs (read-model)
├── exception      # Excepciones y manejo global de errores
├── health         # Health check
├── location       # Ubicaciones
├── maintenance    # Órdenes, reportes de falla, comentarios y adjuntos
├── notification   # Eventos y correo
├── report         # Exportación CSV/PDF (read-model)
├── role           # Roles
├── security       # JWT, filtros y usuario actual
└── user           # Usuarios
```

Dentro de un módulo de negocio:

```
<modulo>
├── controller     # Adaptador HTTP: valida y delega
├── service        # Casos de uso (interfaz)
│   └── impl       # Implementación transaccional
├── repository     # Persistencia (Spring Data)
├── entity         # Modelo JPA
├── mapper         # MapStruct: entidad <-> DTO
└── dto
    ├── request
    └── response
```

No hay un paquete por clase: un paquete agrupa un concepto cohesivo. La regla es
que **lo que cambia junto vive junto**.

## Capas y dependencias

```
controller → service → repository → entity
     │            │
     └── dto ─────┘
```

- Los controladores no conocen repositorios ni entidades JPA: solo servicios,
  DTOs y enums de dominio.
- Los repositorios no conocen servicios, controladores ni mappers.
- Las entidades no dependen de capas superiores.
- Los mappers no dependen de repositorios ni servicios.
- La inyección es siempre por constructor (Lombok `@RequiredArgsConstructor`);
  no se permite `@Autowired` en campos.

## Dependencias entre módulos

El mapa permitido está declarado en
`src/test/java/com/maintena/architecture/ArchitectureTest.java` y se verifica en
cada `mvn test`. Los módulos `common`, `config`, `exception` y `security` son
transversales y puede usarlos cualquier módulo.

| Módulo | Puede depender de |
|---|---|
| asset | category, location, audit |
| audit | user |
| auth | user, role, audit, notification |
| category | audit |
| dashboard | maintenance, asset, user |
| health | — |
| location | audit |
| maintenance | asset, user, audit, notification |
| notification | user |
| report | maintenance, asset, user, audit |
| role | audit |
| user | role, audit |

Reglas adicionales:

- `dashboard` y `report` son **read-models**: pueden leer repositorios de otros
  módulos, pero no escribir en ellos.
- Los efectos secundarios entre módulos se comunican con **eventos de Spring**
  cuando el emisor no necesita respuesta (por ejemplo,
  `UserDeactivatedEvent`, que `auth` escucha para revocar sesiones).
- Un fallo de correo o auditoría nunca revierte la operación de negocio.

## Verificación automática

`ArchitectureTest` aplica 13 reglas con [ArchUnit](https://www.archunit.org/):

1. Controladores sin acceso a repositorios.
2. Controladores sin entidades JPA.
3. Entidades sin dependencias de capas superiores.
4. Repositorios sin dependencias de capas superiores.
5. Servicios sin dependencias de controladores.
6. Mappers sin dependencias de persistencia.
7. Controladores anotados con `@RestController` y sufijo `Controller`.
8. Repositorios como interfaces.
9. Entidades en paquetes `entity`.
10. Mappers `@Mapper` en paquetes `mapper`.
11. Clases `*ServiceImpl` anotadas con `@Service`.
12. Sin inyección por campo (`@Autowired`).
13. Mapa de dependencias entre módulos respetado.

Para ejecutarlas:

```bash
cd backend
mvn test -Dtest=ArchitectureTest
```

Si una dependencia nueva es legítima, se añade conscientemente al mapa
`DEPENDENCIAS_PERMITIDAS`; si no, el build falla.

## Pruebas y calidad

- **Unitarias:** JUnit 5 + Mockito sobre servicios, mappers, filtros y utilidades.
- **Arquitectura:** `ArchitectureTest` (13 reglas) en cada `mvn test`.
- **Integración:** `@SpringBootTest` + `MockMvc` contra **MySQL 8.4 real** con
  Testcontainers (`AuthFlowIntegrationTest`, `UserDeactivationIntegrationTest`).
  Requieren Docker en marcha; el `DataSource` se inyecta con `@ServiceConnection`.
- **Cobertura:** JaCoCo con umbral de 50 % de líneas y 30 % de ramas en
  `mvn verify`. El reporte HTML queda en `target/site/jacoco/index.html`.
- **Formato:** Spotless (imports sin usar, espacios finales, fin de línea y
  newline final). `mvn spotless:apply` corrige y `mvn verify` lo comprueba.
- **CI:** GitHub Actions ejecuta `mvn verify` y el lint/build del frontend.

## Cómo añadir una funcionalidad

1. Identifica el módulo dueño del concepto. Si es nuevo, crea su paquete y su
   `package-info.java`.
2. Añade `entity`, `repository`, `dto`, `mapper`, `service` e `impl`,
   `controller`.
3. Registra la dependencia en el mapa de `ArchitectureTest` solo si es
   inevitable.
4. Añade pruebas unitarias del servicio y, si toca persistencia, una prueba de
   integración.
5. Documenta el módulo en su `package-info.java`.

## Decisiones

- [ADR 0001: organización por funcionalidad](adr/0001-organizacion-por-funcionalidad.md)
