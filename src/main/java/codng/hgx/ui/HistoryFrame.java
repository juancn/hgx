import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
			String file = null;
					file = matcher.group(2);
					pw.printf("<p style=\"margin-top: 5px; margin-bottom: 5px; margin-left: 10px; margin-right: 10px; font-size: 12px; background: rgb(220,220,250); font-family: 'Lucida Grande';\">%s</p>", file);
					final String colorized = file != null && file.endsWith(".java") ? JavaColorizer.colorizeLine(rawLine) : line;
					pw.printf("<span style=\"font-size: 8px;\">(%4d|    )</span><span style=\"background: rgb(255,238,238);\">%s</span>\n", oldStart, colorized);
					final String colorized = file != null && file.endsWith(".java") ? JavaColorizer.colorizeLine(rawLine) : line;
					pw.printf("<span style=\"font-size: 8px;\">(    |%4d)</span><span style=\"background: rgb(221,255,221);\">%s</span>\n", newStart, colorized);
					final String colorized = file != null && file.endsWith(".java") ? JavaColorizer.colorizeLine(rawLine) : line;
					pw.printf("<span style=\"font-size: 8px;\">(%4d|%4d)</span><span>%s</span>\n",oldStart, newStart, colorized);
	public static String htmlEscape(CharSequence s) {
		return s.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");