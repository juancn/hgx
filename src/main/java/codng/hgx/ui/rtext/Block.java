package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/** Base class for blocks of formatted text */
public abstract class Block<B extends Block> {
	/** The current position */
	protected final Point2D.Float position = new Point2D.Float();

	/** If opaque, the background color is used, otherwise it is considered transparent */
	protected boolean opaque;

	/** Foreground color of this block */
	private Color color = Color.BLACK;

	/** Background color of this block. Ignored if not {@link #opaque}. */
	private Color background = Color.WHITE;

	/** A {@link RichTextView} that owns this block */
	protected final RichTextView richTextView;

	/**
	 * Creates a new Block
	 * @param richTextView the {@link RichTextView} that owns this block.
	 */
	public Block(RichTextView richTextView) {
		this.richTextView = richTextView;
	}

	/**
	 * Sets the foreground color in rgb. Values range from 0 to 255.
	 * @param r red component
	 * @param g green component
	 * @param b lue component
	 * @return this {@link Block} for chaining
	 */
	public B color(int r, int g, int b) {
		return color(new Color(r, g, b));
	}

	/**
	 * Sets the background color in rgb. Values range from 0 to 255.
	 * @param r red component
	 * @param g green component
	 * @param b lue component
	 * @return this {@link Block} for chaining
	 */
	public B background(int r, int g, int b) {
		return background(new Color(r, g, b));
	}

	/**
	 * Sets the foreground color.
	 * @param c the new foreground color.
	 * @return this {@link Block} for chaining
	 */
	public B color(final Color c) {
		color = c;
		return self();
	}

	/**
	 * Sets the background color.
	 * @param c the new foreground color.
	 * @return this {@link Block} for chaining
	 */
	public B background(final Color c) {
		background = c;
		return self();
	}

	/**
	 * Creates a new link from this block to the specified anchor block.
	 * @return a new link to the specified anchor block.
	 */
	public Link linkTo(Block anchor) {
		return new Link(this, anchor);
	}

	/** @return a reference to this {@link Block} */
	@SuppressWarnings("unchecked")
	protected B self() { return (B) this; }

	/**
	 * Returns this block's background color.
	 * @param selected if true, the color returned will be the one corresponding to selected text
	 * @return  this block's background color.
	 */
	public Color getBackgroundColor(boolean selected) {
		return selected ? UIManager.getDefaults().getColor("TextArea.selectionBackground") : background;
	}

	/** @return this block's color */
	public Color getForegroundColor() {
		return color;
	}

	/**
	 * Positions this block
	 * @param x x coordinate
	 * @param y y coordinate
	 * @return this {@link Block} for chaining
	 */
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

	/**
	 * Returns the block at coordinate x or null if not found.
	 * Container blocks might return a block other than the current one.
	 * @param x the x coordinate
	 * @return the block at coordinate x or null if not found
	 */
	protected Block blockAt(float x) {
		return position.x <= x && x <= position.x + width() ? this : null;
	}

	/** @return the block's height */
	abstract float height();

	/** @return the block's width */
	public abstract float width();

	@Override
	public String toString() {
		return "";
	}

	/**
	 * Visits this block
	 * @param visitor a block visitor.
	 */
	public abstract void visit(BlockVisitor visitor);

	/** Ensure that this block is visible in the containing {@link RichTextView} */
	public void scrollToVisible() {
		richTextView.scrollRectToVisible(new Rectangle(
				(int) position.x,
				(int) position.y,
				richTextView.getWidth(),
				richTextView.getHeight()));
	}
}
