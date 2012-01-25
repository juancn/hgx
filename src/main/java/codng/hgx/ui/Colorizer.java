package codng.hgx.ui;

public abstract class Colorizer {
	public abstract String colorizeLine(String line);
	
	public static String htmlEscape(CharSequence s) {
		return s.toString().replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static final Colorizer PLAIN = new Colorizer() {
		@Override
		public String colorizeLine(String line) {
			return htmlEscape(line);
		}
	};
}
