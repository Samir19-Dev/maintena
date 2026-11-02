# ADR 0001: organizar el backend por funcionalidad

- **Estado:** aceptada
- **Fecha:** 2026-11-01

## Contexto

El backend tiene 16 módulos y ~145 clases: activos, usuarios, órdenes,
reportes de falla, auditoría, notificaciones, exportaciones y seguridad. La
alternativa considerada era la organización por capa técnica (`controller`,
`service`, `repository`, `dto` en paquetes únicos), usada en otros proyectos
del mismo autor.

## Decisión

Organizar por **funcionalidad** (`com.maintena.<modulo>`), con las capas dentro
de cada módulo. Los módulos transversales (`common`, `config`, `exception`,
`security`) se comparten.

## Motivos

- Alta cohesión: un cambio de dominio toca una sola carpeta.
- Paquetes técnicos únicos se vuelven difíciles de navegar al crecer (el
  proyecto de referencia tiene 79 DTOs y 47 servicios en una sola carpeta).
- Menos conflictos de merge entre funcionalidades.
- Permite verificar límites entre módulos con ArchUnit.

## Consecuencias

- Más carpetas y un nivel extra de profundidad.
- Hay que declarar explícitamente las dependencias entre módulos.
- El equipo debe conocer la convención; se documenta en
  `docs/ARCHITECTURE.md` y en los `package-info.java`.
- Si un módulo pequeño crece, se puede dividir sin afectar a los demás.

## Alternativas descartadas

- **Por capa técnica:** simple al inicio, pero con `dto` y `service` gigantes a
  medida que crece el dominio.
- **Un paquete por clase:** genera ruido, no aporta cohesión y dificulta la
  navegación.
