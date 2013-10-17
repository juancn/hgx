package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

/** A block that horizontally aligns another block */
public class HBox extends Container<HBox> {
	/** The type of alignment */
	private Align align = Align.LEFT;
	/** The width of the block */
	private float width;

	HBox(Block block, float width) {
		super(block);
		this.width = width;
		opaque = true;
	}

	/**
	 * Make this block right-aligned
	 * @return this {@link Block} for chaining
	 */
	public HBox right() {
		align = Align.RIGHT;
		return this;
	}

	/**
	 * Make this block left-aligned
	 * @return this {@link Block} for chaining
	 */
	public HBox left() {
		align = Align.LEFT;
		return this;
	}

	/**
	 * Make this block centered
	 * @return this {@link Block} for chaining
	 */
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

	/** @return this block's alignment */
	public Align align() {
		return align;
	}
}
