package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

public class Link extends Container<Link> {
	final Block anchor;

	Link(final Block block, final Block anchor) {
		super(block);
		this.anchor = anchor;
	}

	@Override
	protected Block blockAt(float x) {
		final Block block = super.blockAt(x);
		return block != null ? this : null;
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }
}
