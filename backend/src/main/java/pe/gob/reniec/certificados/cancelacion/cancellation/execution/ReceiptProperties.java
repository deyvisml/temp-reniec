package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.nio.file.Path;
import java.time.Duration;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties("app.receipt")
public class ReceiptProperties {
	private Mode mode = Mode.DISABLED;
	private Path storageRoot = Path.of("tmp", "receipts");
	private Duration staleGenerationThreshold = Duration.ofMinutes(1);

	@PostConstruct
	void validate() {
		if (mode == null || storageRoot == null) {
			throw new IllegalStateException("Receipt mode and storage root are required");
		}
		if (staleGenerationThreshold == null || staleGenerationThreshold.isZero()
				|| staleGenerationThreshold.isNegative()) {
			throw new IllegalStateException("app.receipt.stale-generation-threshold must be positive");
		}
	}

	public Mode getMode() { return mode; }
	public void setMode(Mode mode) { this.mode = mode; }
	public Path getStorageRoot() { return storageRoot; }
	public void setStorageRoot(Path storageRoot) { this.storageRoot = storageRoot; }
	public Duration getStaleGenerationThreshold() { return staleGenerationThreshold; }
	public void setStaleGenerationThreshold(Duration value) { staleGenerationThreshold = value; }

	public enum Mode { FILESYSTEM, DISABLED }
}
