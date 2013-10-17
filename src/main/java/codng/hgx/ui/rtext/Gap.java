package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

/** A block that represents a horizontal gap */
public class Gap extends Block<Block> {
	/** The gap */
	private final int gap;

	Gap(RichTextView richTextView, int gap) {
		super(richTextView);
		this.gap = gap;
	}

	@Override
	float height() {
		return 0;
	}

	@Override
	public float width() {
		return gap;
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }
}
