package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.RichTextView.BlockVisitor;

import java.awt.Graphics2D;
import java.awt.geom.Line2D;

public class HRuler extends Block<HRuler> {

	private final float height = 10;
	private final float width;
	private final float hpad = 50;

	HRuler(RichTextView richTextView, float width) {
		super(richTextView);
		this.width = width;
	}

	@Override
	protected void draw(Graphics2D g) {
		super.draw(g);
		g.draw(new Line2D.Float(position.x + hpad/2,position.y, position.x+ width() - hpad/2, position.y));
	}

	@Override
	float height() {
		return height;
	}

	@Override
	public float width() {
		return width;
	}

	@Override
	public void visit(BlockVisitor visitor) { visitor.visit(this); }
}
