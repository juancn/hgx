package codng.hgx.ui;

import codng.hgx.ui.RichTextView.Text;

import java.awt.Color;

public enum TextStyle {
	STRING_LITERAL {{ monospaced().bold().foreground(Colors.STRING_LITERAL); }},
	COMMENT {{ monospaced().italic().foreground(Colors.COMMENT); }},
	NUMBER {{ monospaced().foreground(Colors.NUMBER); }},
	KEYWORD {{ monospaced().foreground(Colors.KEYWORD); }},
	ANNOTATION {{ monospaced().foreground(Colors.ANNOTATION); }},
	CODE {{ monospaced(); }},
	;


	private boolean monospaced;
	private boolean bold;
	private boolean italic;
	private boolean underline;
	private int fontSize = 12;
	private Color foreground = Color.BLACK;
	private Color background = Color.WHITE;

	protected TextStyle monospaced() { monospaced = true; return this; }
	protected TextStyle bold() { bold = true; return this; }
	protected TextStyle italic() { italic = true; return this; }
	protected TextStyle underline() { underline = true; return this; }
	protected TextStyle foreground(final Color c) { foreground = c; return this; }
	protected TextStyle background(final Color c) { background = c; return this; }

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
}
