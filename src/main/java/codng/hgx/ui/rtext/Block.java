package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

public abstract class Block<B extends Block> {
	protected final Point2D.Float position = new Point2D.Float();
	protected boolean opaque;
	private Color color = Color.BLACK;
	private Color background = Color.WHITE;

	protected final RichTextView richTextView;

	public Block(RichTextView richTextView) {
		this.richTextView = richTextView;
	}

	public B color(int r, int g, int b) {
		return color(new Color(r, g, b));
	}

	public B background(int r, int g, int b) {
		return background(new Color(r, g, b));
	}

	public B color(final Color c) {
		color = c;
		return self();
	}

	public B background(final Color c) {
		background = c;
		return self();
	}

	public Link linkTo(Block anchor) {
		return new Link(this, anchor);
	}

	@SuppressWarnings("unchecked")
	protected B self() { return (B) this; }

	public Color getBackgroundColor(boolean selected) {
		return selected ? UIManager.getDefaults().getColor("TextArea.selectionBackground") : background;
	}

	public Color getForegroundColor() {
		return color;
	}

	protected B position(float x, float y) {
		position.x = x;
		position.y = y;
		return self();
	}

	protected void draw(Graphics2D g) {
		final float h = height();
		final float w = width();
		final boolean selected;
		if(richTextView.endBlock != null && richTextView.startBlock != null) {
			// Only line selection is supported
			selected = (richTextView.startBlock.position.y <= position.y && position.y <= richTextView.endBlock.position.y)
					|| (richTextView.endBlock.position.y <= position.y && position.y <= richTextView.startBlock.position.y);
		} else {
			selected =false;
		}

		if (opaque || selected) {
			g.setColor(getBackgroundColor(selected));
			g.fill(new Rectangle2D.Float(position.x, position.y, w, h));
		}
		g.setColor(getForegroundColor());
	}

	protected Block blockAt(float x) {
		return position.x <= x && x <= position.x + width() ? this : null;
	}

	abstract float height();
	public abstract float width();

	@Override
	public String toString() {
		return "";
	}

	public abstract void visit(BlockVisitor visitor);
}
