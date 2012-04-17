package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

public class Text extends Block<Text> {
	private String text;
	private float vgap = 5;
	private float hgap = 5;

	private boolean bold;
	private boolean monospaced;
	private boolean italic;
	private boolean underline;
	private int size = 12;

	Text(RichTextView richTextView, String text) {
		super(richTextView);
		text(text);
	}

	public void text(String text) {
		// Quick & Dirty fix for tabs
		this.text = RichTextView.TAB_PATTERN.matcher(text).replaceAll("    ");
	}

	public String text() {
		return this.text;
	}

	public Text monospaced() {
		return monospaced(true);
	}

	public Text monospaced(final boolean monospaced) {
		this.monospaced = monospaced;
		return this;
	}

	public Text bold() {
		return bold(true);
	}

	public Text bold(final boolean v) {
		this.bold = v;
		return this;
	}

	public Text size(final int size) {
		this.size = size;
		return this;
	}

	public int size() {
		return size;
	}

	public Text italic() {
		return italic(true);
	}

	public Text italic(final boolean v) {
		italic = v;
		return this;
	}

	public Text underline() {
		return underline(true);
	}

	public Text underline(final boolean v) {
		underline = v;
		return this;
	}

	public Text hgap(float hgap) {
		this.hgap = hgap;
		return this;
	}

	private FontMetrics fontMetrics() {
		return richTextView.fontMetrics(monospaced, bold, italic, underline, size);
	}

	private Font font() {
		return richTextView.font(monospaced, bold, italic, underline, size);
	}

	@Override
	float height() {
		return fontMetrics().getHeight() + vgap;
	}

	@Override
	protected void draw(Graphics2D g) {
		super.draw(g);
		g.setFont(font());
		final int ascent = fontMetrics().getAscent();
		g.drawString(text, position.x+hgap/2 , position.y + vgap/2 + ascent);
	}

	@Override
	public float width() {
		return fontMetrics().stringWidth(text) + hgap;
	}

	public Text vgap(final float vgap) {
		this.vgap = vgap;
		return this;
	}

	public float vgap() {
		return vgap;
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }

	@Override
	public String toString() {
		return hgap == 0?text : " " + text;
	}

	public boolean isMonospaced() {
		return monospaced;
	}

	public boolean isBold() {
		return bold;
	}

	public boolean isItalic() {
		return italic;
	}

	public boolean isUnderline() {
		return underline;
	}

	public float hgap() {
		return hgap;
	}
}
