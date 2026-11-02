package com.maintena.maintenance.storage;

import com.maintena.exception.BadRequestException;
import com.maintena.exception.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Guarda los adjuntos en disco con nombres aleatorios y valida tipo y tamaño.
 * La ruta pública nunca se construye con datos del cliente.
 */
@Component
@Slf4j
public class AttachmentStorage {
	private static final long DEFAULT_MAX_BYTES = 5L * 1024 * 1024;
	private static final int MAX_NAME_LENGTH = 255;

	private static final Map<String, String> EXTENSION_TO_TYPE = Map.of(
		"png", "image/png",
		"jpg", "image/jpeg",
		"jpeg", "image/jpeg",
		"webp", "image/webp",
		"gif", "image/gif",
		"pdf", "application/pdf"
	);

	private static final Map<String, String> TYPE_TO_EXTENSION = Map.of(
		"image/png", "png",
		"image/jpeg", "jpg",
		"image/webp", "webp",
		"image/gif", "gif",
		"application/pdf", "pdf"
	);

	private final Path baseDir;
	private final long maxSizeBytes;

	public AttachmentStorage(
		@Value("${app.attachments.dir:uploads}") String dir,
		@Value("${app.attachments.max-size-bytes:5242880}") long maxSizeBytes
	) {
		this.baseDir = Paths.get(dir).toAbsolutePath().normalize();
		this.maxSizeBytes = maxSizeBytes > 0 ? maxSizeBytes : DEFAULT_MAX_BYTES;
	}

	public StoredAttachment store(Long orderId, MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("El archivo está vacío");
		}
		if (file.getSize() > maxSizeBytes) {
			throw new BadRequestException("El archivo supera el máximo de "
				+ Math.max(1, maxSizeBytes / (1024 * 1024)) + " MB");
		}

		String nombreOriginal = sanitizeName(file.getOriginalFilename());
		String tipoContenido = resolveContentType(nombreOriginal, file.getContentType());
		String storedName = UUID.randomUUID() + "." + TYPE_TO_EXTENSION.get(tipoContenido);

		Path directorio = baseDir.resolve("orders").resolve(String.valueOf(orderId)).normalize();
		if (!directorio.startsWith(baseDir)) {
			throw new IllegalStateException("Ruta de adjuntos inválida");
		}

		try {
			Files.createDirectories(directorio);
			Path destino = directorio.resolve(storedName).normalize();
			try (InputStream entrada = file.getInputStream()) {
				Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
			}
			return new StoredAttachment(storedName, nombreOriginal, tipoContenido, file.getSize());
		} catch (IOException ex) {
			throw new IllegalStateException("No se pudo guardar el adjunto", ex);
		}
	}

	public Resource load(Long orderId, String storedName) {
		Path archivo = resolve(orderId, storedName);
		if (!Files.isRegularFile(archivo)) {
			throw new ResourceNotFoundException("Adjunto", "archivo", storedName);
		}
		return new FileSystemResource(archivo);
	}

	public void delete(Long orderId, String storedName) {
		try {
			Files.deleteIfExists(resolve(orderId, storedName));
		} catch (IOException ex) {
			log.warn("No se pudo borrar el adjunto {} de la orden {}: {}", storedName, orderId, ex.getMessage());
		}
	}

	private Path resolve(Long orderId, String storedName) {
		Path archivo = baseDir.resolve("orders").resolve(String.valueOf(orderId))
			.resolve(storedName).normalize();
		if (!archivo.startsWith(baseDir)) {
			throw new ResourceNotFoundException("Adjunto", "archivo", storedName);
		}
		return archivo;
	}

	private String resolveContentType(String nombreOriginal, String declarado) {
		String extension = extension(nombreOriginal);
		String porExtension = extension == null ? null : EXTENSION_TO_TYPE.get(extension);
		if (porExtension != null) {
			return porExtension;
		}

		String limpio = declarado == null ? "" : declarado.toLowerCase(Locale.ROOT).split(";")[0].trim();
		if (TYPE_TO_EXTENSION.containsKey(limpio)) {
			return limpio;
		}

		throw new BadRequestException("Solo se permiten imágenes (PNG, JPG, WEBP, GIF) o archivos PDF");
	}

	private String extension(String nombre) {
		int punto = nombre.lastIndexOf('.');
		if (punto < 0 || punto == nombre.length() - 1) {
			return null;
		}
		return nombre.substring(punto + 1).toLowerCase(Locale.ROOT);
	}

	private String sanitizeName(String nombre) {
		if (nombre == null || nombre.isBlank()) {
			return "archivo";
		}
		String limpio;
		try {
			limpio = Paths.get(nombre).getFileName().toString();
		} catch (InvalidPathException ex) {
			limpio = nombre;
		}
		limpio = limpio.replaceAll("[\\\\/\\r\\n\\t\\u0000]", " ").trim();
		if (limpio.isEmpty() || ".".equals(limpio) || "..".equals(limpio)) {
			return "archivo";
		}
		if (limpio.length() > MAX_NAME_LENGTH) {
			limpio = limpio.substring(0, MAX_NAME_LENGTH);
		}
		return limpio;
	}

	public record StoredAttachment(
		String storedName,
		String nombreOriginal,
		String tipoContenido,
		long tamano
	) {
	}
}
