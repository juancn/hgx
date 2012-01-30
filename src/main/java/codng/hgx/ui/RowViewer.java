package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.Row;

import javax.swing.JComponent;
import javax.swing.Scrollable;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
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
	
	private Point selectionStart;
	private Point selectionEnd;

	public RowViewer() {
		this(null);
	}

	public RowViewer(Row row) {
		this.row = row;
		final MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					selectionStart = e.getPoint();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					selectionEnd = e.getPoint();
					repaint();
				}
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					selectionEnd = e.getPoint();
					repaint();
				}
			}
		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);
		
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
		selectionStart = null;
		selectionEnd = null;
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
		float totalHeight = 0, maxWidth = 0, maxHeight = 0;
		for (Strip line : lines) {
			final float lineHeight = line.height();
			line.position(0, totalHeight);
			totalHeight += lineHeight;
			maxHeight += max(maxHeight, totalHeight);
			maxWidth = max(maxWidth, line.width());				
		}
		setPreferredSize(new Dimension((int)maxWidth, (int)totalHeight));
	}
	
	private Block blockAt(float x, float y) {
		return lines.get(lineAt(y)).blockAt(x);
	}

	private int lineAt(float y) {
		int index = Collections.binarySearch(lines, y, new Comparator<Object>() {
			@Override
			public int compare(Object o1, Object o2) {
				return Float.compare(toFloat(o1), toFloat(o2));
			}

			private float toFloat(Object o) {
				return o instanceof Float ? (Float) o : ((Block) o).position.y;
			}
		});

		if(index < 0) {
			index = -index -1;
		} 
		
		// We always select the next row, because position.y is at the top of the box, so we correct for that.
		if (index != 0) {
			index = index - 1;
		}
		return index;
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
			
			boolean skipDiff = false;
			Colorizer colorizer = Colorizer.plain(this);
			for(String line = br.readLine(); line != null; line = br.readLine())  {

				if(line.startsWith("diff")) {
					skipDiff = false;
					final Matcher matcher = DIFF_PATTERN.matcher(line);
					if(!matcher.matches()) throw new IllegalArgumentException("Malformed diff");
					final String file = matcher.group(2);
					line().add(align(text(file).vgap(10).bold(), getParent().getWidth()-50).background(220, 220, 250));
				
					if(file.endsWith(".java")) {
						colorizer = new JavaColorizer(this);
					} else {
						colorizer = Colorizer.plain(this);
					}
				} else if(line.startsWith("new file mode")) { // I should check that we're still in the header
					line().add(code(line).rgb(127, 127, 127));
				} else if(line.startsWith("deleted file mode")) {
					line().add(code(line).rgb(127, 127, 127));
					line().add(text("File deleted").rgb(255, 0, 0));
				} else if(line.startsWith("index ")) {
					line().add(code(line).rgb(127, 127, 127));
				} else if(line.startsWith("Binary file ")) {
					skipDiff = true;
				} else if(line.startsWith("GIT binary patch")) {
					line().add(text("(Binary file, content not rendered)").rgb(127, 127, 127));
					skipDiff = true;
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
				} else if(!skipDiff) {
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
			float height() {
				return 0;
			}

			@Override
			float width() {
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
		final Rectangle clipBounds = g.getClipBounds();
		final int y1 = clipBounds.y;
		final int y2 = clipBounds.y + clipBounds.height;

		float y = y1;
		for (int i = lineAt(y1); i < lines.size() && y <= y2; i++) {
			final Strip line = lines.get(i);
			y = line.position.y;
			if ( (y1 <= y && y <= y2) ) {
				line.draw(g);
			}
		}
	}
	
	
	abstract class Block<B extends Block> {
		protected final Point2D.Float position = new Point2D.Float(); 
		protected boolean opaque;
		private Color color = Color.BLACK;
		private Color background = Color.WHITE;
		
		@SuppressWarnings("unchecked")
		B rgb(int r, int g, int b) {
			color = new Color(r, g, b);
			return self();
		}

		B background(int r, int g, int b) {
			background = new Color(r, g, b);
			return self();
		}

		@SuppressWarnings("unchecked")
		protected B self() { return (B) this; }

		protected Color background(boolean selected) {
			return selected ? UIManager.getDefaults().getColor("TextArea.selectionBackground") : background;
		}

		protected Color color(boolean selected) {
			return selected ? color : color;
		}
		
		protected B position(float x, float y) {
			position.x = x;
			position.y = y;
			return self();
		}

		protected void draw(Graphics2D g) {
			final float h = height();
			final float w = width();
			final boolean selected = (selectionStart != null && RowViewer.this.blockAt(selectionStart.x, selectionStart.y) == this) 
					|| (selectionEnd != null && RowViewer.this.blockAt(selectionEnd.x, selectionEnd.y) == this);
					
			if (opaque || selected) {
				g.setColor(background(selected));
				g.fill(new Rectangle2D.Float(position.x, position.y, w, h));
			}
			g.setColor(color(selected));
		}
		
		protected Block blockAt(float x) {
			return position.x <= x && x <= position.x + width() ? this : null;
		}

		abstract float height();
		abstract float width();
	}

	class Strip extends Block<Strip> {
		private List<Block> blocks = new ArrayList<>();
		private float hgap = 0;

		Strip() {
			opaque = true;
		}

		Strip add(Block... values) {
			blocks.addAll(Arrays.asList(values));
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
			for (Block block : blocks) {
				block.draw(g);
			}
		}

		@Override
		float width() {
			float width = hgap;
			for (Block block : blocks) {
				width += block.width();
			}
			return width;
		}
		
		Strip hgap(final float hgap) {
			this.hgap = hgap;
			return this;
		}

		@Override
		protected Strip position(float x, float y) {
			super.position(x, y);
			float xoff = x + hgap/2;
			for (Block block : blocks) {
				block.position(xoff, y);
				xoff += block.width();
			}
			return this;
		}

		@Override
		protected Block blockAt(float x) {
			for (Block block : blocks) {
				final Block b = block.blockAt(x);
				if(b != null) return b;
			}
			return super.blockAt(x);
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

		private FontMetrics fontMetrics() {
			return getGraphics().getFontMetrics(font());
		}

		private Font font() {
			Font font = monospaced ? monospacedFont() : variableFont();
			if (bold) font = font.deriveFont(Font.BOLD);
			if (italic) font = font.deriveFont(Font.ITALIC);
			if (size != font.getSize()) font = font.deriveFont((float)size);
			return font;
		}

		@Override
		float height() {
			return fontMetrics().getHeight() + vgap;
		}

		@Override
		protected void draw(Graphics2D g) {
			super.draw(g);
			g.setFont(font());
			final int ascent = fontMetrics().getAscent();
			g.drawString(text, position.x+hgap/2 , position.y + vgap/2 + ascent);
		}

		@Override
		float width() {
			return fontMetrics().stringWidth(text) + hgap;
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
			opaque = true;
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
		protected void draw(Graphics2D g) {
			super.draw(g);
			block.draw(g);
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
		float height() {
			return block.height();
		}

		@Override
		float width() {
			return width;
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
		protected void draw(Graphics2D g) {
			super.draw(g);
			g.draw(new Line2D.Float(position.x + hpad/2,position.y, position.x+ width() - hpad/2, position.y));
		}

		@Override
		float height() {
			return height;
		}

		@Override
		float width() {
			return width;
		}
	}
	
	enum Align { LEFT, CENTER, RIGHT }
	
	static Font monospacedFont() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Monaco");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}

	static Font variableFont() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Lucida Grande");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}
	
	private static Pattern DIFF_PATTERN = Pattern.compile("diff --git a/(.*) b/(.*)");
	private static Pattern HUNK_PATTERN = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
	
}
