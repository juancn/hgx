package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

public class HBox extends Container<HBox> {
	private Align align = Align.LEFT;
	private float width;

	HBox(Block block, float width) {
		super(block);
		this.width = width;
		opaque = true;
	}

	public HBox right() {
		align = Align.RIGHT;
		return this;
	}

	public HBox left() {
		align = Align.LEFT;
		return this;
	}

	public HBox center() {
		align = Align.CENTER;
		return this;
	}

	@Override
	protected HBox position(float x, float y) {
		super.position(x, y);
		final float blockWidth = block.width();
		float xoff = 0;
		if(width > blockWidth) {
			switch (align) {
				case CENTER:
					xoff = (width - blockWidth)/2;
					break;
				case RIGHT:
					xoff = (width - blockWidth);
					break;
			}
		}
		block.position(x + xoff, y);
		return this;
	}

	@Override
	public float width() {
		return width;
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }

	public Align align() {
		return align;
	}
}
