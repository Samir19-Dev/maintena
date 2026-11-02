package com.maintena.maintenance.dto;

import org.springframework.core.io.Resource;

public record OrderAttachmentFile(
	Resource resource,
	String nombreOriginal,
	String tipoContenido,
	long tamano
) {
}
