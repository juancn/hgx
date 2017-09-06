package codng.hgx.ui.rtext;

import codng.hgx.ui.TextStyle;

import java.util.ArrayList;
import java.util.List;

public class RichTextViewModel<V extends RichTextView> {
	protected final List<Strip> lines = new ArrayList<>();
	protected V richTextView;

	public RichTextViewModel(V richTextView) {
		this.richTextView = richTextView;
	}

	public void clear() {
		lines.clear();
	}

	public Strip line() {
		final Strip line = strip().lpad(25);
		lines.add(line);
		return line;
	}

	protected void hr() {
		lines.add(strip().add(new HRuler(richTextView, richTextView.getParent().getWidth())));
	}

	protected void header(String label, Block value) {
		lines.add(strip().add(align(TextStyle.LABEL.applyTo(text(label)), 100).right(), value));
	}

	protected void header(String label, Object value) {
		header(label, text(value));
	}

	protected Block<Block> gap(final int gap) {
		return new Gap(richTextView, gap);
	}

	public Text code(Object line) {
		return TextStyle.CODE.applyTo(text(line));
	}

	public Strip strip() {
		return new Strip(richTextView);
	}

	protected HBox align(Block block, float width) {
		return new HBox(block, width);
	}

	public Text text(Object value) {
		return new Text(richTextView, String.valueOf(value));
	}

	public List<Strip> getLines() {
		return lines;
	}
}
