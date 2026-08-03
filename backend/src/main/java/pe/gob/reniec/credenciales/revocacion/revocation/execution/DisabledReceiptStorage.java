package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.io.IOException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.receipt", name = "mode", havingValue = "disabled", matchIfMissing = true)
public class DisabledReceiptStorage implements ReceiptStorage {
	@Override
	public String store(String receiptCode, byte[] document) throws IOException {
		throw new IOException("Receipt storage is not configured");
	}

	@Override
	public byte[] read(String storageReference) throws IOException {
		throw new IOException("Receipt storage is not configured");
	}
}
