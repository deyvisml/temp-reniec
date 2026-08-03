package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilesystemReceiptStorageTests {

	@TempDir
	Path temporaryDirectory;

	@Test
	void usesThePermanentLocalStorageDirectoryByDefault() {
		assertThat(new ReceiptProperties().getStorageRoot())
				.isEqualTo(Path.of("storage", "receipts"));
	}

	@Test
	void storesAndReadsAReceiptWithoutLeavingPartialFiles() throws Exception {
		Path storageRoot = temporaryDirectory.resolve("storage").resolve("receipts");
		ReceiptProperties properties = new ReceiptProperties();
		properties.setStorageRoot(storageRoot);
		FilesystemReceiptStorage storage = new FilesystemReceiptStorage(properties);
		byte[] document = "%PDF-1.7 durable receipt".getBytes(StandardCharsets.US_ASCII);

		String reference = storage.store("RV-2026-000001", document);

		assertThat(reference).isEqualTo("RV-2026-000001.pdf");
		assertThat(storage.read(reference)).isEqualTo(document);
		try (var files = Files.list(storageRoot)) {
			assertThat(files.map(Path::getFileName)).containsExactly(Path.of(reference));
		}
	}
}
