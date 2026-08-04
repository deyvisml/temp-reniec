package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.junit.jupiter.api.Test;

class RevocationReceiptPdfRendererTests {

	@Test
	void rendersAOnePageInstitutionalReceiptUsingOnlySupportedEvidence() throws Exception {
		RevocationReceiptPdfRenderer renderer = new RevocationReceiptPdfRenderer();
		byte[] document = renderer.render(new RevocationReceiptPdfRenderer.Data(
				"RV-2026-000128", "73905791", "José Luis", 31,
				Instant.parse("2026-07-15T15:24:00Z"), "Robo", null,
				Instant.parse("2026-07-30T18:00:00Z"),
				Instant.parse("2026-07-30T18:00:01Z")));

		Path preview = Path.of("target", "receipt-preview.pdf");
		Files.createDirectories(preview.getParent());
		Files.write(preview, document);

		try (var pdf = Loader.loadPDF(document)) {
			TitleFontSizeStripper textStripper = new TitleFontSizeStripper();
			String text = textStripper.getText(pdf);
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 144), "PNG",
					Path.of("target", "receipt-preview.png").toFile());
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(textStripper.titleFontSizes).isNotEmpty()
					.allMatch(size -> Math.abs(size - 15f) < 0.01f);
			assertThat(pdf.getPage(0).getResources().getXObjectNames()).anySatisfy(name -> {
				try {
					assertThat(pdf.getPage(0).getResources().getXObject(name))
							.isInstanceOfSatisfying(PDImageXObject.class, image -> {
								assertThat(image.getWidth()).isEqualTo(301);
								assertThat(image.getHeight()).isEqualTo(132);
							});
				}
				catch (IOException exception) {
					throw new AssertionError(exception);
				}
			});
			assertThat(text).contains("CONSTANCIA DE REVOCACIÓN DE CREDENCIAL VERIFICABLE",
					"REGISTRO NACIONAL DE IDENTIFICACIÓN Y ESTADO CIVIL",
					"NOMBRE VERIFICADO (PRIMER NOMBRE)", "José Luis",
					"DNI 73905791", "CÓDIGO DE CONSTANCIA", "RV-2026-000128",
					"ESTADO FINAL", "REVOCADA", "ÍNDICE DE CREDENCIAL",
					"31", "MOTIVO", "Robo", "FECHA DE REVOCACIÓN",
					"30 de julio de 2026", "13:00:01 UTC-5",
					"únicamente la credencial verificable seleccionada",
					"Documento generado electrónicamente");
			assertThat(text).doesNotContain("******", "UUID", "11111111-1111", "ID Perú",
					"QR", "SHA256", "Ley N", "firma", "PROCESADO", "AUTORIZADO",
					"provisional", "evidencia legal", "Entidad de Certificación");
		}
	}

	@Test
	void wrapsLongOtherReasonAndReplacesUnsupportedCharacters() throws Exception {
		String longReason = "Solicito la revocación porque el equipo anterior quedó fuera de mi control "
				+ "y necesito evitar cualquier uso no autorizado de la credencial asociado. "
				+ "Información adicional con acentos: pérdida, protección y teléfono. "
				+ "Detalle complementario para comprobar el ajuste correcto de un texto extenso "
				+ "sin invadir el bloque informativo ubicado al final del documento. 🔒";
		String longFirstName = "María de los Ángeles del Carmen Alejandra Fernanda Isabel "
				+ "Guadalupe Esperanza Victoria";
		RevocationReceiptPdfRenderer renderer = new RevocationReceiptPdfRenderer();

		byte[] document = renderer.render(new RevocationReceiptPdfRenderer.Data(
				"RV-2026-000129", "73905791", longFirstName, 31,
				Instant.parse("2026-07-15T15:24:00Z"), "Otro motivo", longReason,
				Instant.parse("2026-07-30T18:00:00Z"),
				Instant.parse("2026-07-30T18:00:01Z")));

		try (var pdf = Loader.loadPDF(document)) {
			String text = new PDFTextStripper().getText(pdf);
			String normalizedText = text.replaceAll("\\s+", " ");
			ImageIO.write(new PDFRenderer(pdf).renderImageWithDPI(0, 144), "PNG",
					Path.of("target", "receipt-long-preview.png").toFile());
			assertThat(pdf.getNumberOfPages()).isEqualTo(1);
			assertThat(normalizedText).contains("Solicito la revocación", "uso no autorizado",
					"pérdida, protección y teléfono", "María de los Ángeles", "?");
			assertThat(text).doesNotContain("ID Perú", "UUID", "QR", "provisional");
		}
	}

	private static final class TitleFontSizeStripper extends PDFTextStripper {
		private static final String TITLE = "CONSTANCIA DE REVOCACIÓN DE CREDENCIAL VERIFICABLE";
		private final List<Float> titleFontSizes = new ArrayList<>();

		private TitleFontSizeStripper() throws IOException { }

		@Override
		protected void writeString(String text, List<TextPosition> textPositions)
				throws IOException {
			if (text.contains(TITLE)) {
				textPositions.forEach(position -> titleFontSizes.add(position.getFontSizeInPt()));
			}
			super.writeString(text, textPositions);
		}
	}
}
