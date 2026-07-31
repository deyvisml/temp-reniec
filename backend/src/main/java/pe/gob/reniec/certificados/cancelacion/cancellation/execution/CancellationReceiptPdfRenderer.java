package pe.gob.reniec.certificados.cancelacion.cancellation.execution;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

@Component
public class CancellationReceiptPdfRenderer {
	private static final Color NAVY = new Color(6, 26, 80);
	private static final Color BLUE = new Color(7, 85, 223);
	private static final Color MUTED = new Color(66, 91, 142);
	private static final DateTimeFormatter DATE = DateTimeFormatter
			.ofPattern("dd/MM/yyyy - HH:mm").withZone(ZoneId.of("America/Lima"));
	private static final float VALUE_WIDTH = 285;
	private static final float ROW_FONT_SIZE = 9;
	private static final float ROW_LINE_HEIGHT = 12;
	private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	public byte[] render(Data data) throws IOException {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				header(content);
				text(content, bold, 22, NAVY, 54, 720, "Constancia provisional de cancelación");
				text(content, regular, 10, MUTED, 54, 698,
						"Resultado de la cancelación del certificado digital seleccionado");
				line(content, 54, 680, 541, 680, new Color(220, 229, 242));

				int y = 645;
				y = row(content, y, "Código de constancia", data.receiptCode());
				y = row(content, y, "DNI", data.maskedDni());
				y = row(content, y, "N.° de orden del certificado", data.orderNumber());
				y = row(content, y, "Creado el", DATE.format(data.certificateCreatedAt()));
				y = row(content, y, "Motivo", data.reasonLabel());
				if (data.otherReason() != null && !data.otherReason().isBlank()) {
					y = row(content, y, "Descripción adicional", data.otherReason());
				}
				y = row(content, y, "Confirmado el", DATE.format(data.confirmedAt()));
				y = row(content, y, "Cancelado el", DATE.format(data.completedAt()));
				row(content, y, "Resultado", "CANCELACIÓN EXITOSA");

				content.setNonStrokingColor(new Color(240, 246, 255));
				content.addRect(54, 175, 487, 76);
				content.fill();
				text(content, bold, 10, NAVY, 72, 224,
						"Se canceló únicamente el certificado digital seleccionado.");
				text(content, regular, 9, MUTED, 72, 203,
						"Esta acción no afecta tu DNI ni tu identidad civil.");
				text(content, regular, 8, MUTED, 54, 92,
						"Documento provisional sujeto a validación institucional. No contiene firma digital ni QR.");
				text(content, regular, 8, MUTED, 54, 72,
						"Generado por el Sistema de Gestión de Certificados Digitales de RENIEC.");
			}
			document.save(output);
			return output.toByteArray();
		}
	}

	private void header(PDPageContentStream content) throws IOException {
		content.setNonStrokingColor(NAVY);
		content.addRect(0, 770, PDRectangle.A4.getWidth(), 72);
		content.fill();
		text(content, bold, 20, Color.WHITE, 54, 798, "RENIEC");
		text(content, regular, 10, Color.WHITE, 142, 798,
				"Sistema de Gestión de Certificados Digitales");
	}

	private int row(PDPageContentStream content, int y, String label, String value) throws IOException {
		text(content, bold, 9, NAVY, 64, y, label);
		List<String> lines = wrap(regular, value, ROW_FONT_SIZE, VALUE_WIDTH);
		for (int index = 0; index < lines.size(); index++) {
			text(content, regular, ROW_FONT_SIZE, MUTED, 250,
					y - (index * ROW_LINE_HEIGHT), lines.get(index));
		}
		int height = Math.max(42, 24 + Math.round(lines.size() * ROW_LINE_HEIGHT));
		line(content, 54, y - height + 27, 541, y - height + 27,
				new Color(227, 234, 244));
		return y - height;
	}

	private static void text(PDPageContentStream content, PDType1Font font, float size,
			Color color, float x, float y, String value) throws IOException {
		content.beginText();
		content.setFont(font, size);
		content.setNonStrokingColor(color);
		content.newLineAtOffset(x, y);
		content.showText(sanitize(value));
		content.endText();
	}

	private static void line(PDPageContentStream content, float x1, float y1, float x2,
			float y2, Color color) throws IOException {
		content.setStrokingColor(color);
		content.setLineWidth(0.7f);
		content.moveTo(x1, y1);
		content.lineTo(x2, y2);
		content.stroke();
	}

	private static List<String> wrap(PDType1Font font, String value, float size,
			float maxWidth) throws IOException {
		String normalized = sanitize(value).replaceAll("\\s+", " ").trim();
		if (normalized.isEmpty()) return List.of("");
		List<String> lines = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		for (int index = 0; index < normalized.length(); index++) {
			char character = normalized.charAt(index);
			String candidate = current.toString() + character;
			if (!current.isEmpty() && width(font, candidate, size) > maxWidth) {
				lines.add(current.toString().stripTrailing());
				current.setLength(0);
				if (character != ' ') current.append(character);
			}
			else {
				current.append(character);
			}
		}
		if (!current.isEmpty()) lines.add(current.toString().stripTrailing());
		return lines.isEmpty() ? List.of("") : List.copyOf(lines);
	}

	private static float width(PDType1Font font, String value, float size) throws IOException {
		return font.getStringWidth(value) / 1000f * size;
	}

	private static String sanitize(String value) {
		if (value == null) return "";
		String normalized = value.replace('–', '-').replace('—', '-')
				.replace('“', '"').replace('”', '"').replace('’', '\'');
		StringBuilder safe = new StringBuilder(normalized.length());
		normalized.codePoints().forEach(codePoint -> {
			if (codePoint == '\n' || codePoint == '\r' || codePoint == '\t') {
				safe.append(' ');
			}
			else if (codePoint >= 32 && codePoint <= 255
					&& !Character.isISOControl(codePoint)) {
				safe.appendCodePoint(codePoint);
			}
			else {
				safe.append('?');
			}
		});
		return safe.toString();
	}

	public record Data(String receiptCode, String maskedDni, String orderNumber,
			Instant certificateCreatedAt, String reasonLabel, String otherReason,
			Instant confirmedAt, Instant completedAt) { }
}
