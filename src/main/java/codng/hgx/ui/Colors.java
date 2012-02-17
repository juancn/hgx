package codng.hgx.ui;

import java.awt.Color;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Colors {
	// Code
	public static final Color COMMENT = color("comment",128, 128, 128);
	public static final Color STRING_LITERAL = color("string",0, 128, 0);
	public static final Color ANNOTATION = color("annotation",128, 128, 0);
	public static final Color NUMBER = color("number",0, 0, 255);
	public static final Color RESERVED = color("reserved",0, 0, 128);

	// Informational
	public static final Color WARNING = color("warning",255, 0, 0);
	public static final Color DE_EMPHASIZE = color("de-emphasize",127, 127, 127);
	public static final Color LOADING = color("loading",200, 200, 200);

	// Background
	public static final Color REMOVED_BG = color("background.removed",255, 238, 238);
	public static final Color ADDED_BG = color("background.added",221, 255, 221);
	public static final Color FILE_BG = color("background.file",220, 220, 250);
	public static final Color LINE_NO_BG = color("background.line-no",250, 250, 250);

	private static Color color(final String key, final int r, final int g, final int b) {
		return parse(key, new Color(r,g,b));
	}

	private static Color parse(final String key, final Color defaultColor) {
		final String property = System.getProperty(key);
		if (property != null) {
			// Values are of the form "rgb(255,0,0)"
			final Matcher matcher = Pattern.compile("rgb\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)").matcher(property);
			if(matcher.matches()) {
				return new Color(Integer.parseInt(matcher.group(1)),Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(3)));
			}
		}
		return defaultColor;
	}

}
