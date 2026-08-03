package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.nio.file.Path;
import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.receipt")
public class ReceiptProperties {
	private Mode mode = Mode.DISABLED;
	private Path storageRoot = Path.of("storage", "receipts");
	private Duration staleGenerationThreshold = Duration.ofMinutes(1);
	private Duration processingInterval = Duration.ofSeconds(5);
	private boolean processingEnabled;
	private boolean requireAbsoluteStorageRoot = true;

	@PostConstruct
	void validate() {
		if (mode == null || storageRoot == null) {
			throw new IllegalStateException("Receipt mode and storage root are required");
		}
		if (staleGenerationThreshold == null || staleGenerationThreshold.isZero()
				|| staleGenerationThreshold.isNegative()) {
			throw new IllegalStateException("app.receipt.stale-generation-threshold must be positive");
		}
		if (processingInterval == null || processingInterval.isZero() || processingInterval.isNegative()) {
			throw new IllegalStateException("app.receipt.processing-interval must be positive");
		}
		if (mode == Mode.FILESYSTEM && requireAbsoluteStorageRoot && !storageRoot.isAbsolute()) {
			throw new IllegalStateException("app.receipt.storage-root must be absolute outside local/test profiles");
		}
	}

	public Mode getMode() { return mode; }
	public void setMode(Mode mode) { this.mode = mode; }
	public Path getStorageRoot() { return storageRoot; }
	public void setStorageRoot(Path storageRoot) { this.storageRoot = storageRoot; }
	public Duration getStaleGenerationThreshold() { return staleGenerationThreshold; }
	public void setStaleGenerationThreshold(Duration value) { staleGenerationThreshold = value; }
	public Duration getProcessingInterval() { return processingInterval; }
	public void setProcessingInterval(Duration value) { processingInterval = value; }
	public boolean isProcessingEnabled() { return processingEnabled; }
	public void setProcessingEnabled(boolean value) { processingEnabled = value; }
	public boolean isRequireAbsoluteStorageRoot() { return requireAbsoluteStorageRoot; }
	public void setRequireAbsoluteStorageRoot(boolean value) { requireAbsoluteStorageRoot = value; }

	public enum Mode { FILESYSTEM, DISABLED }
}
