package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.receipt", name = "mode", havingValue = "filesystem")
public class FilesystemReceiptStorage implements ReceiptStorage {
	private final Path root;

	public FilesystemReceiptStorage(ReceiptProperties properties) {
		root = properties.getStorageRoot().toAbsolutePath().normalize();
	}

	@Override
	public String store(String receiptCode, byte[] document) throws IOException {
		Files.createDirectories(root);
		Path target = resolve(receiptCode + ".pdf");
		Path temporary = Files.createTempFile(root, receiptCode + "-", ".tmp");
		try {
			Files.write(temporary, document);
			try {
				Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE,
						StandardCopyOption.REPLACE_EXISTING);
			}
			catch (java.nio.file.AtomicMoveNotSupportedException ignored) {
				Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
			}
		}
		finally {
			Files.deleteIfExists(temporary);
		}
		return target.getFileName().toString();
	}

	@Override
	public byte[] read(String storageReference) throws IOException {
		return Files.readAllBytes(resolve(storageReference));
	}

	private Path resolve(String name) {
		if (name == null || name.isBlank()) throw new IllegalArgumentException("Invalid storage reference");
		Path relative = Path.of(name);
		if (relative.isAbsolute() || relative.getNameCount() != 1
				|| !name.toLowerCase(java.util.Locale.ROOT).endsWith(".pdf")) {
			throw new IllegalArgumentException("Invalid storage reference");
		}
		Path resolved = root.resolve(relative).normalize();
		if (!resolved.startsWith(root)) throw new IllegalArgumentException("Invalid storage reference");
		return resolved;
	}
}
