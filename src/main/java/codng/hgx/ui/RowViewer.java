package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.Row;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Math.max;

public class RowViewer 
	extends JComponent
	implements Scrollable
{
	private Row row;
	private List<Strip> lines = new ArrayList<>();

	public RowViewer() {
		this(null);
	}

	public RowViewer(Row row) {
		this.row = row;
	}

	public Row getRow() {
		return row;
	}

	public void setRow(Row row) {
		this.row = row;
		recalculate();
		revalidate();
		repaint();
	}

	private void recalculate() {
		lines.clear();
		if(row != null) {
			header("SHA:", row.changeSet.id);
			header("Author:", row.changeSet.user);
			header("Date:", row.changeSet.date);
			header("Summary:", text(row.changeSet.summary).bold());
			header("Parent:", row.changeSet.parents);
			header("Branch:", text(row.changeSet.branch).bold());
			hr();
			try {
				colorize(Cache.loadDiff(row));
			} catch (IOException e) {
				e.printStackTrace();
			}
			updateDimensions();
		}
	}

	private void updateDimensions() {
		final Graphics2D g = (Graphics2D) getGraphics();
		float totalHeight = 0, maxWidth = 0, maxHeight = 0;
		for (Strip line : lines) {
			final float lineHeight = line.getHeight(g);
			totalHeight += lineHeight;
			maxHeight += max(maxHeight, totalHeight);
			maxWidth = max(maxWidth, line.getWidth(g));				
		}
		setPreferredSize(new Dimension((int)maxWidth, (int)totalHeight));
	}

	@Override
	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	@Override
	public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 15;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return 500;
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	private void colorize(String diff) {
		final StringReader sr = new StringReader(diff);
		final BufferedReader br = new BufferedReader(sr);
		try {
			int oldStart = -1, newStart = -1;
			
			Colorizer colorizer = Colorizer.plain(this);
			for(String line = br.readLine(); line != null; line = br.readLine())  {

				if(line.startsWith("diff")) {
					final Matcher matcher = DIFF_PATTERN.matcher(line);
					if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
					final String file = matcher.group(2);
					line().add(align(text(file).vgap(10).bold(), getParent().getWidth()-50).background(220, 220, 250));
				
					if(file.endsWith(".java")) {
						colorizer = new JavaColorizer(this);
					} else {
						colorizer = Colorizer.plain(this);
					}
				} else if(line.startsWith("new file mode")) {
					line().add(code(line).rgb(127, 127, 127));
				} else if(line.startsWith("+++")) {
					// Don't care 
				} else if(line.startsWith("---")) {
					// Don't care 
				} else if(line.startsWith("@@")) {
					final Matcher matcher = HUNK_PATTERN.matcher(line);
					if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
					oldStart = Integer.parseInt(matcher.group(1));
					newStart = Integer.parseInt(matcher.group(3));
					line().add(code(line).rgb(127, 127, 127));
					colorizer.reset();
				} else if(line.startsWith("-")) {
					numbered(oldStart, -1, colorizer.colorizeLine(line).background(255, 238, 238));
					++oldStart;
				} else if(line.startsWith("+")) {
					numbered(-1, newStart, colorizer.colorizeLine(line).background(221, 255, 221));
					++newStart;
				} else {
					numbered(oldStart, newStart, colorizer.colorizeLine(line));
					++oldStart; ++newStart;
				}
			}
		} catch (IOException e) {
			throw new Error("This shouldn't happen!");
		}
	}

	private Strip numbered(int oldStart, int newStart, Block block) {
		return line()
				.add(align(code(oldStart == -1? "" : oldStart).size(10), 30).right().background(250, 250, 250))
				.add(gap(2))
				.add(align(code(newStart == -1 ? "" : newStart).size(10), 30).right().background(250, 250, 250))
				.add(block);
	}

	private Block<Block> gap(final int gap) {
		return new Block<Block>() {
			@Override
			float getHeight(Graphics2D g) {
				return 0;
			}

			@Override
			float getWidth(Graphics2D g) {
				return gap;
			}
		};
	}

	Text code(Object line) {
		return text(line).monospaced();
	}

	Strip line() {
		final Strip line = strip().hgap(50);
		lines.add(line);
		return line;
	}

	Strip strip() {
		return new Strip();
	}

	private void hr() {
		lines.add(strip().add(new HRuler(getParent().getWidth())));
	}

	private void header(String label, Text value) {
		lines.add(strip().add(align(text(label).rgb(127, 127, 127).bold(), 100).right(), value));
	}

	private void header(String label, Object value) {
		header(label, text(value));
	}


	private HBox align(Block block, float width) {
		return new HBox(block, width);
	}

	private Text text(Object value) {
		return new Text(String.valueOf(value));
	}

	@Override
	protected void paintComponent(Graphics g) {
		// Alow my UI to paint itself
		super.paintComponent(g);
		render((Graphics2D) g);
	}

	private void render(Graphics2D g) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		float y = 0;
		final Rectangle clipBounds = g.getClipBounds();
		final int y1 = clipBounds.y;
		final int y2 = clipBounds.y + clipBounds.height;

		for (int i = 0; i < lines.size() && y <= y2; i++) {
			final Strip line = lines.get(i);

			if ( (y1 <= y && y <= y2) ) {
				line.drawAt(0, y, g);
			}

			y += line.getHeight(g);
		}
	}
	
	
	abstract class Block<B extends Block> {
		void drawAt(float x, float y, Graphics2D g) {
			g.setColor(background);
			g.fill(new Rectangle2D.Float(x, y, getWidth(g), getHeight(g)));
			g.setColor(color);
		}
		
		abstract float getHeight(Graphics2D g);
		abstract float getWidth(Graphics2D g);

		protected Color color = Color.BLACK;
		protected Color background = Color.WHITE;
		
		@SuppressWarnings("unchecked")
		B rgb(int r, int g, int b) {
			color = new Color(r, g, b);
			return (B) this;
		}

		@SuppressWarnings("unchecked")
		B background(int r, int g, int b) {
			background = new Color(r, g, b);
			return (B) this;
		}
	}

	class Strip extends Block<Strip> {
		private List<Block> blocks = new ArrayList<>();
		private float hgap = 0;
		
		
		Strip add(Block... values) {
			blocks.addAll(Arrays.asList(values));
			return this;
		}

		@Override
		float getHeight(Graphics2D g) {
			float height = 0;
			for (Block block : blocks) {
				height = max(height, block.getHeight(g));
			}
			return height;
		}

		@Override
		void drawAt(float x, float y, Graphics2D g) {
			float xoff = x + hgap/2;
			for (Block block : blocks) {
				block.drawAt(xoff, y, g);
				xoff += block.getWidth(g);
			}
		}

		@Override
		float getWidth(Graphics2D g) {
			float width = hgap;
			for (Block block : blocks) {
				width += block.getWidth(g);
			}
			return width;
		}

		@Override
		Strip background(int r, int g, int b) {
			for (Block block : blocks) {
				block.background(r, g, b);
			}
			return super.background(r, g, b);
		}
		
		Strip hgap(final float hgap) {
			this.hgap = hgap;
			return this;
		}
	}
	
	class Text extends Block<Text> {
		private final String text;
		private float vgap = 5;
		private float hgap = 5;
		
		private boolean bold;
		private boolean monospaced;
		private boolean italic;
		private int size = 12;

		Text(String text) {
			this.text = text;
		}

		public Text bold() {
			bold = true;
			return this;
		}

		public Text size(final int size) {
			this.size = size;
			return this;
		}
		
		public Text italic() {
			italic = true;
			return this;
		}

		public Text hgap(float hgap) {
			this.hgap = hgap;
			return this;
		}

		private FontMetrics fontMetrics(Graphics2D g) {
			return g.getFontMetrics(font());
		}

		private Font font() {
			Font font = monospaced ? RowViewer.monospaced() : variable();
			if (bold) font = font.deriveFont(Font.BOLD);
			if (italic) font = font.deriveFont(Font.ITALIC);
			if (size != font.getSize()) font = font.deriveFont((float)size);
			return font;
		}

		@Override
		float getHeight(Graphics2D g) {
			return fontMetrics(g).getHeight() + vgap;
		}

		@Override
		void drawAt(float x, float y, Graphics2D g) {
			super.drawAt(x, y, g);
			g.setFont(font());
			final int ascent = fontMetrics(g).getAscent();
			g.drawString(text, x+hgap/2 , y + vgap/2 + ascent);
		}

		@Override
		float getWidth(Graphics2D g) {
			return fontMetrics(g).stringWidth(text) + hgap;
		}

		public Text monospaced() {
			monospaced = true;
			return this;
		}

		public Text vgap(final float vgap) {
			this.vgap = vgap;
			return this;
		}
	}
	
	class HBox extends Block<HBox> {
		private Align align = Align.LEFT;
		private Block block;
		private float width;

		HBox(Block block, float width) {
			this.block = block;
			this.width = width;
		}

		HBox right() {
			align = Align.RIGHT;
			return this;
		}

		HBox left() {
			align = Align.LEFT;
			return this;
		}

		HBox center() {
			align = Align.CENTER;
			return this;
		}
		
		@Override
		void drawAt(float x, float y, Graphics2D g) {
			super.drawAt(x, y, g);
			final float blockWidth = block.getWidth(g);
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
			block.drawAt(x + xoff, y, g);
		}

		@Override
		float getHeight(Graphics2D g) {
			return block.getHeight(g);
		}

		@Override
		float getWidth(Graphics2D g) {
			return width;
		}

		@Override
		HBox background(int r, int g, int b) {
			block.background(r, g, b);
			return super.background(r, g, b);
		}
	}
	
	class HRuler extends Block<HRuler> {

		private final float height = 10;
		private final float width;
		private final float hpad = 50;

		HRuler(float width) {
			this.width = width;
		}

		@Override
		void drawAt(float x, float y, Graphics2D g) {
			super.drawAt(x, y, g);
			g.draw(new Line2D.Float(x + hpad/2,y, x+getWidth(g) - hpad/2, y));
		}

		@Override
		float getHeight(Graphics2D g) {
			return height;
		}

		@Override
		float getWidth(Graphics2D g) {
			return width;
		}
	}
	
	enum Align { LEFT, CENTER, RIGHT }
	
	static Font monospaced() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Monaco");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}

	static Font variable() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Lucida Grande");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}
	
	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
	
}
