package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.io.IOException;

public interface ReceiptStorage {
	String store(String receiptCode, byte[] document) throws IOException;
	byte[] read(String storageReference) throws IOException;
}
