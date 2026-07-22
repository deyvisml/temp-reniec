package pe.gob.reniec.certificados.cancelacion.cancellation.initiation;

import java.util.regex.Pattern;

public final class DniRule {

	public static final String REGEX = "[0-9]{8}";
	private static final Pattern PATTERN = Pattern.compile(REGEX);

	private DniRule() { }

	public static boolean isValid(String value) {
		return value != null && PATTERN.matcher(value).matches();
	}

	public static String masked(String value) {
		if (!isValid(value)) throw new IllegalArgumentException("Invalid DNI");
		return "******" + value.substring(6);
	}
}
