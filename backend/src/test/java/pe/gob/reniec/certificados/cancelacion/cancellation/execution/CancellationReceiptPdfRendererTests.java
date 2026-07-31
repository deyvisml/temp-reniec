package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;

class CancellationReceiptPdfRendererTests {

	@Test
	void rendersAOnePageProvisionalReceiptWithoutSensitiveUuid() throws Exception {
		CancellationReceiptPdfRenderer renderer = new CancellationReceiptPdfRenderer();
		byte[] document = renderer.render(new CancellationReceiptPdfRenderer.Data(
				"CD-2026-000128", "******91", "0000123456",
				Instant.parse("2026-07-15T15:24:00Z"), "Robo", null,
				Instant.parse("2026-07-30T18:00:00Z"),
				Instant.parse("2026-07-30T18:00:01Z")));

		Path preview = Path.of("target", "receipt-preview.pdf");
		Files.createDirectories(preview.getParent());
		Files.write(preview, document);

		try (var pdf = Loader.loadPDF(document)) {
			String text = new PDFTextStripper().getText(pdf);
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(text).contains("Constancia provisional de cancelación",
					"CD-2026-000128", "******91", "0000123456",
					"CANCELACIÓN EXITOSA", "únicamente el certificado digital seleccionado");
			assertThat(text).doesNotContain("UUID", "11111111-1111", "Identidad", "ID Perú");
		}
	}

	@Test
	void wrapsLongOtherReasonAndReplacesUnsupportedCharacters() throws Exception {
		String longReason = "Solicito la cancelación porque el equipo anterior quedó fuera de mi control "
				+ "y necesito evitar cualquier uso no autorizado del certificado asociado. "
				+ "Información adicional con acentos: pérdida, protección y teléfono. 🔒";
		CancellationReceiptPdfRenderer renderer = new CancellationReceiptPdfRenderer();

		byte[] document = renderer.render(new CancellationReceiptPdfRenderer.Data(
				"CD-2026-000129", "******91", "0000123456",
				Instant.parse("2026-07-15T15:24:00Z"), "Otro motivo", longReason,
				Instant.parse("2026-07-30T18:00:00Z"),
				Instant.parse("2026-07-30T18:00:01Z")));

		try (var pdf = Loader.loadPDF(document)) {
			String text = new PDFTextStripper().getText(pdf);
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(text).contains("Solicito la cancelación", "uso no autorizado",
					"pérdida, protección y teléfono", "?");
			assertThat(text).doesNotContain("Identidad", "ID Perú");
		}
	}
}
