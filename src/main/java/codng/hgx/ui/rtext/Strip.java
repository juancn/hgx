package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;
import codng.util.Sequence;
import codng.util.Sequences;

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static java.lang.Math.max;

public class Strip extends Block<Strip> {
	private List<Block> blocks = new ArrayList<>();
	private float lpad = 0;

	Strip(final RichTextView richTextView) {
		super(richTextView);
		opaque = true;
	}

	public Strip add(Block... values) {
		blocks.addAll(Arrays.asList(values));
		return this;
	}

	public Strip add(Sequence<Block> values) {
		blocks.addAll(values.toList());
		return this;
	}

	@Override
	float height() {
		float height = 0;
		for (Block block : blocks) {
			height = max(height, block.height());
		}
		return height;
	}

	@Override
	protected void draw(Graphics2D g) {
		super.draw(g);
		final Rectangle clipBounds = g.getClipBounds();
		for (Block block : blocks) {
			if(block.position.x > clipBounds.x+clipBounds.width) break;
			block.draw(g);
		}
	}

	@Override
	public float width() {
		float width = lpad;
		for (Block block : blocks) {
			width += block.width();
		}
		return width;
	}

	Strip lpad(final float lpad) {
		this.lpad = lpad;
		return this;
	}

	@Override
	protected Strip position(float x, float y) {
		super.position(x, y);
		float xoff = x + lpad;
		for (Block block : blocks) {
			block.position(xoff, y);
			xoff += block.width();
		}
		return this;
	}

	@Override
	protected Block blockAt(float x) {
		int index = Collections.binarySearch(blocks, x, RichTextView.X_COMPARATOR);
		if(index < 0) {
			index = -index-1;
		}

		// Correct index, we look at the base x position, so we're shifted
		index -= 1;
		return index == -1 ? super.blockAt(x) : blocks.get(index).blockAt(x);
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder();
		for (int i = 0; i < blocks.size(); i++) {
			Block block = blocks.get(i);
			sb.append(block);
		}
		return sb.toString();
	}

	public List<Block> blocks() {
		return blocks;
	}
}
