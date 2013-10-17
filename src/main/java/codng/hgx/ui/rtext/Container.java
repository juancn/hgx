package codng.hgx.ui.rtext;

import java.awt.Color;
import java.awt.Graphics2D;

/** Base class for blocks that contain and modify another block */
public abstract class Container<T extends Container> extends Block<T> {
	/** The contained block */
	public final Block block;

	Container(Block block) {
		super(block.richTextView);
		this.block = block;
	}

	@Override
	protected void draw(Graphics2D g) {
		super.draw(g);
		block.draw(g);
	}

	@Override
	float height() {
		return block.height();
	}

	@Override
	public float width() {
		return block.width();
	}

	@Override
	protected T position(float x, float y) {
		block.position(x, y);
		return super.position(x, y);
	}

	@Override
	public T color(Color c) {
		block.color(c);
		return super.color(c);
	}

	@Override
	public T background(Color c) {
		block.background(c);
		return super.background(c);
	}

	@Override
	protected Block blockAt(float x) {
		return block.blockAt(x);
	}

	@Override
	public String toString() {
		return block.toString();
	}
}
