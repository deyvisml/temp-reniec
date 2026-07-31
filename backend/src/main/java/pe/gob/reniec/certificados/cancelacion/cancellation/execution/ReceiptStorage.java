package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.io.IOException;

public interface ReceiptStorage {
	String store(String receiptCode, byte[] document) throws IOException;
	byte[] read(String storageReference) throws IOException;
}
