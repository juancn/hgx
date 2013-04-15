package codng.hgx.ui;

import codng.hgx.ui.rtext.Text;

import java.awt.Color;

public enum TextStyle {
	STRING_LITERAL("string") {{ monospaced().bold().foreground(Colors.STRING_LITERAL); }},
	COMMENT("comment") {{ monospaced().italic().foreground(Colors.COMMENT); }},
	NUMBER("number") {{ monospaced().foreground(Colors.NUMBER); }},
	KEYWORD("keyword") {{ monospaced().foreground(Colors.KEYWORD); }},
	DIRECTIVE("directive") {{ monospaced().foreground(Colors.DIRECTIVE); }},
	ANNOTATION("annotation") {{ monospaced().foreground(Colors.ANNOTATION); }},
	CODE("code") {{ monospaced(); }},
	LINK("link") {{ underline().foreground(Colors.LINK); }},
	LABEL("label") {{ bold().foreground(Colors.DE_EMPHASIZE); }},
	;

	private final String name;
	private boolean monospaced;
	private boolean bold;
	private boolean italic;
	private boolean underline;
	private int fontSize = 12;
	private Color foreground = Color.BLACK;
	private Color background = Color.WHITE;

	private TextStyle(String name) { this.name = name; }

	protected TextStyle monospaced() { monospaced = true; return this; }
	protected TextStyle bold() { bold = true; return this; }
	protected TextStyle italic() { italic = true; return this; }
	protected TextStyle underline() { underline = true; return this; }
	protected TextStyle foreground(final Color c) { foreground = c; return this; }
	protected TextStyle background(final Color c) { background = c; return this; }
	protected TextStyle size(final int size) { fontSize = size; return this; }

	public Text applyTo(final Text text) {
		return text
			.monospaced(monospaced)
			.bold(bold)
			.italic(italic)
			.underline(underline)
			.size(fontSize)
			.color(foreground)
			.background(background);
	}

	private void loadCustom() {
		monospaced = bool("monospaced", monospaced);
		bold = bool("bold", bold);
		italic = bool("italic", italic);
		underline = bool("italic", underline);
		size(Integer.getInteger(property("size"), fontSize));
		foreground(Colors.getColor(property("foreground"), foreground));
		background(Colors.getColor(property("background"), background));
	}

	private boolean bool(final String property, final boolean defVal) {
		return Boolean.parseBoolean(System.getProperty(property(property), String.valueOf(defVal)));
	}

	private String property(final String property) { return name + "." + property; }


	static { 
		for (TextStyle style : values()) {
			style.loadCustom();
		}
	}
}
