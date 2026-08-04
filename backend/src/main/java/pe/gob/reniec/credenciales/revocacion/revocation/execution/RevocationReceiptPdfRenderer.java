package pe.gob.reniec.credenciales.revocacion.revocation.execution;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.stereotype.Component;

@Component
public class RevocationReceiptPdfRenderer {
	private static final Color NAVY = new Color(7, 35, 67);
	private static final Color BURGUNDY = new Color(138, 48, 85);
	private static final Color RED = new Color(187, 31, 35);
	private static final Color TEXT = new Color(35, 39, 45);
	private static final Color MUTED = new Color(105, 113, 124);
	private static final Color LINE = new Color(211, 216, 223);
	private static final Color NOTICE_BACKGROUND = new Color(252, 246, 248);
	private static final ZoneId LIMA = ZoneId.of("America/Lima");
	private static final Locale SPANISH_PERU = Locale.forLanguageTag("es-PE");
	private static final DateTimeFormatter DATE_TIME = DateTimeFormatter
			.ofPattern("dd/MM/yyyy - HH:mm", SPANISH_PERU).withZone(LIMA);
	private static final DateTimeFormatter LONG_DATE = DateTimeFormatter
			.ofPattern("d 'de' MMMM 'de' yyyy", SPANISH_PERU).withZone(LIMA);
	private static final DateTimeFormatter EXACT_TIME = DateTimeFormatter
			.ofPattern("HH:mm:ss 'UTC-5'", SPANISH_PERU).withZone(LIMA);
	private static final String LOGO_RESOURCE = "/pdf/reniec-logo.png";
	private static final String TITLE = "CONSTANCIA DE REVOCACIÓN DE CREDENCIAL VERIFICABLE";
	private static final float PAGE_MARGIN = 43.5f;
	private static final float PAGE_LEFT = PAGE_MARGIN;
	private static final float PAGE_RIGHT = PDRectangle.A4.getWidth() - PAGE_MARGIN;
	private static final float CONTENT_WIDTH = PAGE_RIGHT - PAGE_LEFT;
	private static final float COLUMN_GAP = 24;
	private static final float COLUMN_WIDTH = (CONTENT_WIDTH - COLUMN_GAP) / 2;
	private static final float FIELD_VALUE_SIZE = 10;
	private static final float FIELD_LINE_HEIGHT = 12.5f;
	private static final float LOGO_WIDTH = 86;
	private static final float LOGO_HEIGHT = LOGO_WIDTH * 132 / 301;
	private static final float TITLE_SIZE = 15;
	private final PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
	private final PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

	public byte[] render(Data data) throws IOException {
		try (PDDocument document = new PDDocument();
				ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			PDPage page = new PDPage(PDRectangle.A4);
			document.addPage(page);
			PDImageXObject logo = loadLogo(document);
			try (PDPageContentStream content = new PDPageContentStream(document, page)) {
				header(content, logo);
				centeredUnderlinedText(content, bold, TITLE_SIZE, NAVY, BURGUNDY, 681,
						TITLE);

				float introductionBottom = paragraph(content, regular, 10, TEXT, PAGE_LEFT, 631,
						CONTENT_WIDTH, 15,
						"El Registro Nacional de Identificación y Estado Civil (RENIEC) deja constancia "
								+ "de que la credencial verificable detallada a continuación fue revocada "
								+ "satisfactoriamente a solicitud del ciudadano autenticado.");

				float top = introductionBottom - 22;
				line(content, PAGE_LEFT, top, PAGE_RIGHT, top, LINE, 0.8f);
				top = gridRow(content, top, "NOMBRE VERIFICADO (PRIMER NOMBRE)", data.firstName(),
						"DOCUMENTO DE IDENTIDAD", "DNI " + data.dni(), false);
				top = gridRow(content, top, "CÓDIGO DE CONSTANCIA", data.receiptCode(),
						"ESTADO FINAL", "REVOCADA", true);
				top = gridRow(content, top, "ÍNDICE DE CREDENCIAL", Integer.toString(data.statusListIndex()),
						"FECHA DE CREACIÓN", DATE_TIME.format(data.digitalCredentialCreatedAt()), false);
				top = gridRow(content, top, "MOTIVO", data.reasonLabel(),
						"SOLICITUD CONFIRMADA", DATE_TIME.format(data.confirmedAt()), false);
				if (data.otherReason() != null && !data.otherReason().isBlank()) {
					top = fullWidthRow(content, top, "DESCRIPCIÓN ADICIONAL", data.otherReason());
				}
				float detailsBottom = gridRow(content, top, "FECHA DE REVOCACIÓN", LONG_DATE.format(data.completedAt()),
						"HORA EXACTA", EXACT_TIME.format(data.completedAt()), false);

				informationNotice(content, Math.max(178, detailsBottom - 28));
				line(content, PAGE_LEFT, 74, PAGE_RIGHT, 74, LINE, 0.7f);
				centeredText(content, regular, 7, MUTED, 52,
						"Documento generado electrónicamente por el Sistema de Gestión de Credenciales Verificables de RENIEC.");
			}
			document.save(output);
			return output.toByteArray();
		}
	}

	private void header(PDPageContentStream content, PDImageXObject logo) throws IOException {
		content.drawImage(logo, PAGE_LEFT, 761, LOGO_WIDTH, LOGO_HEIGHT);
		text(content, bold, 6.8f, MUTED, PAGE_LEFT, 744,
				"REGISTRO NACIONAL DE IDENTIFICACIÓN Y ESTADO CIVIL");
	}

	private PDImageXObject loadLogo(PDDocument document) throws IOException {
		try (InputStream input = RevocationReceiptPdfRenderer.class.getResourceAsStream(LOGO_RESOURCE)) {
			if (input == null) {
				throw new IOException("No se encontró el recurso institucional " + LOGO_RESOURCE);
			}
			return PDImageXObject.createFromByteArray(document, input.readAllBytes(), "reniec-logo");
		}
	}

	private float gridRow(PDPageContentStream content, float top, String leftLabel,
			String leftValue, String rightLabel, String rightValue, boolean highlightRight)
			throws IOException {
		List<String> leftLines = wrap(bold, leftValue, FIELD_VALUE_SIZE, COLUMN_WIDTH);
		List<String> rightLines = wrap(bold, rightValue, FIELD_VALUE_SIZE, COLUMN_WIDTH);
		int valueLines = Math.max(leftLines.size(), rightLines.size());
		float height = Math.max(52, 34 + valueLines * FIELD_LINE_HEIGHT);
		field(content, PAGE_LEFT, top, COLUMN_WIDTH, leftLabel, leftLines, TEXT);
		field(content, PAGE_LEFT + COLUMN_WIDTH + COLUMN_GAP, top, COLUMN_WIDTH,
				rightLabel, rightLines, highlightRight ? RED : TEXT);
		line(content, PAGE_LEFT, top - height, PAGE_RIGHT, top - height, LINE, 0.7f);
		return top - height;
	}

	private float fullWidthRow(PDPageContentStream content, float top, String label, String value)
			throws IOException {
		List<String> valueLines = wrap(regular, value, 9.5f, CONTENT_WIDTH - 12);
		float height = Math.max(52, 34 + valueLines.size() * 12);
		text(content, bold, 7.25f, MUTED, PAGE_LEFT, top - 17, label);
		for (int index = 0; index < valueLines.size(); index++) {
			text(content, regular, 9.5f, TEXT, PAGE_LEFT, top - 35 - index * 12,
					valueLines.get(index));
		}
		line(content, PAGE_LEFT, top - height, PAGE_RIGHT, top - height, LINE, 0.7f);
		return top - height;
	}

	private void field(PDPageContentStream content, float x, float top, float width,
			String label, List<String> valueLines, Color valueColor) throws IOException {
		text(content, bold, 7.25f, MUTED, x, top - 17, label);
		for (int index = 0; index < valueLines.size(); index++) {
			text(content, bold, FIELD_VALUE_SIZE, valueColor, x,
					top - 35 - index * FIELD_LINE_HEIGHT, valueLines.get(index));
		}
	}

	private void informationNotice(PDPageContentStream content, float top) throws IOException {
		float bottom = top - 72;
		content.setNonStrokingColor(NOTICE_BACKGROUND);
		content.addRect(PAGE_LEFT, bottom, CONTENT_WIDTH, 72);
		content.fill();
		content.setNonStrokingColor(RED);
		content.addRect(PAGE_LEFT, bottom, 3, 72);
		content.fill();
		text(content, bold, 9, NAVY, PAGE_LEFT + 18, top - 24,
				"Se revocó únicamente la credencial verificable seleccionada.");
		text(content, regular, 8.5f, MUTED, PAGE_LEFT + 18, top - 44,
				"Esta acción no afecta tu DNI ni tu identidad civil.");
	}

	private void centeredUnderlinedText(PDPageContentStream content, PDType1Font font,
			float size, Color textColor, Color underlineColor, float y, String value)
			throws IOException {
		String safe = sanitize(value);
		float textWidth = width(font, safe, size);
		float x = (PDRectangle.A4.getWidth() - textWidth) / 2;
		text(content, font, size, textColor, x, y, safe);
		line(content, x, y - 5.5f, x + textWidth, y - 5.5f, underlineColor, 0.9f);
	}

	private void centeredText(PDPageContentStream content, PDType1Font font, float size,
			Color color, float y, String value) throws IOException {
		String safe = sanitize(value);
		float x = (PDRectangle.A4.getWidth() - width(font, safe, size)) / 2;
		text(content, font, size, color, x, y, safe);
	}

	private float paragraph(PDPageContentStream content, PDType1Font font, float size,
			Color color, float x, float y, float maxWidth, float lineHeight, String value)
			throws IOException {
		List<String> lines = wrap(font, value, size, maxWidth);
		for (int index = 0; index < lines.size(); index++) {
			text(content, font, size, color, x, y - index * lineHeight, lines.get(index));
		}
		return y - lines.size() * lineHeight;
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
			float y2, Color color, float width) throws IOException {
		content.setStrokingColor(color);
		content.setLineWidth(width);
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
		for (String word : normalized.split(" ")) {
			String candidate = current.isEmpty() ? word : current + " " + word;
			if (!current.isEmpty() && width(font, candidate, size) > maxWidth) {
				lines.add(current.toString());
				current.setLength(0);
			}
			appendFitted(lines, current, word, font, size, maxWidth);
		}
		if (!current.isEmpty()) lines.add(current.toString());
		return lines.isEmpty() ? List.of("") : List.copyOf(lines);
	}

	private static void appendFitted(List<String> lines, StringBuilder current, String word,
			PDType1Font font, float size, float maxWidth) throws IOException {
		if (!current.isEmpty()) current.append(' ');
		for (int index = 0; index < word.length(); index++) {
			char character = word.charAt(index);
			String candidate = current.toString() + character;
			if (!current.isEmpty() && width(font, candidate, size) > maxWidth) {
				lines.add(current.toString().stripTrailing());
				current.setLength(0);
			}
			current.append(character);
		}
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

	public record Data(String receiptCode, String dni, String firstName, int statusListIndex,
			Instant digitalCredentialCreatedAt, String reasonLabel, String otherReason,
			Instant confirmedAt, Instant completedAt) { }
}
