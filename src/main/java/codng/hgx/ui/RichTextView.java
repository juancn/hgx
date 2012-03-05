package codng.hgx.ui;

import codng.util.DefaultFunction;
import codng.util.Tuple;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.text.TextAction;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.Math.max;

public class RichTextView extends JComponent implements Scrollable {
	private Model model = new Model();
	protected Block startBlock;
	protected Point selectionStart;
	protected Block endBlock;
	protected Point selectionEnd;

	private float avgLineHeight = 20;

	public RichTextView() {
		final MouseAdapter mouseAdapter = new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					startBlock = blockAt(e.getX(), e.getY());
					selectionStart = e.getPoint();
				}
				requestFocusInWindow();
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				mouseReleased(e);
			}

			@Override
			public void mouseReleased(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					endBlock = blockAt(e.getX(), e.getY());
					selectionEnd = e.getPoint();
					repaint();
				}
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getButton() == MouseEvent.BUTTON1) {
					final Block block = blockAt(e.getX(), e.getY());
					if (block instanceof Link) {
						final Link link = (Link) block;
						clearSelection();
						normalCursor();
						scrollRectToVisible(new Rectangle(
								(int) link.anchor.position.x,
								(int) link.anchor.position.y,
								getWidth(),
								getHeight()));
					}
				}
			}

			/** Used to avoid changing the cursor too often */
			boolean handOn;
			@Override
			public void mouseMoved(MouseEvent e) {
				final Block block = blockAt(e.getX(), e.getY());
				final boolean onLink = block instanceof Link;
				if (onLink && !handOn) {
					this.handOn = true;
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
				} else if(!onLink && handOn) {
					normalCursor();
				}
			}

			private void normalCursor() {
				this.handOn = false;
				setCursor(Cursor.getDefaultCursor());
			}
		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);

		initCopy();
	}

	private void clearSelection() {
		startBlock = endBlock = null;
	}

	private void initCopy() {
		final DataFlavor html = new DataFlavor("text/html", "Html");
		if (OS_X) {
			// Java for OSX doesn't seem to have a native registered for HTML
			final SystemFlavorMap flavorMap = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
			flavorMap.addFlavorForUnencodedNative("public.html", html);
			flavorMap.addUnencodedNativeForFlavor(html, "public.html");
		}

		setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(JComponent c) {
				return COPY;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new Transferable() {
					private final DataFlavor[] dataFlavors = {
							html,
							new DataFlavor("text/plain", "Plain text"),
					};

					@Override
					public DataFlavor[] getTransferDataFlavors() {
						return dataFlavors;
					}

					@Override
					public boolean isDataFlavorSupported(DataFlavor flavor) {
						return Arrays.asList(dataFlavors).contains(flavor);
					}

					@Override
					public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
						return serializeText(flavor.getMimeType().contains("text/html") ? getSelectedHtml() : getSelectedText());
					}

					private Object serializeText(String selectedText) throws IOException {
						return new ByteArrayInputStream(selectedText.getBytes("UTF-8"));
					}
				};
			}
		});
		setFocusable(true);

		// Install the copy action
		final Object copyActionName = addAction(TransferHandler.getCopyAction());
		getInputMap().put(KeyStroke.getKeyStroke("ctrl C"), copyActionName);
		getInputMap().put(KeyStroke.getKeyStroke("meta C"), copyActionName);

		final Object selectAll = addAction(new TextAction("selectAll") {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!model.lines.isEmpty()) {
					final Strip lastLine = model.lines.get(model.lines.size() - 1);
					selectionStart = new Point(0,0);
					startBlock = blockAt(selectionStart.x, selectionStart.y);
					selectionEnd = new Point((int) lastLine.width(), (int) (lastLine.position.y + lastLine.height()));
					endBlock = blockAt(selectionEnd.x, selectionEnd.y);
					repaint();
				}
			}
		});
		getInputMap().put(KeyStroke.getKeyStroke("ctrl A"), selectAll);
		getInputMap().put(KeyStroke.getKeyStroke("meta A"), selectAll);
	}
	
	public void clear() {
		model.clear();
		clearSelection();
	}

	public class Model {
		protected final List<Strip> lines = new ArrayList<>();

		public Model() { }

		public void clear() {
			lines.clear();
		}

		Strip line() {
			final Strip line = strip().lpad(25);
			lines.add(line);
			return line;
		}

		protected void hr() {
			lines.add(strip().add(new HRuler(getParent().getWidth())));
		}

		protected void header(String label, Text value) {
			lines.add(strip().add(align(TextStyle.LABEL.applyTo(text(label)), 100).right(), value));
		}

		protected void header(String label, Object value) {
			header(label, text(value));
		}

		protected Block<Block> gap(final int gap) {
			return new Gap(gap);
		}

		Text code(Object line) {
			return TextStyle.CODE.applyTo(text(line));
		}

		Strip strip() {
			return new Strip();
		}

		protected HBox align(Block block, float width) {
			return new HBox(block, width);
		}

		protected Text text(Object value) {
			return new Text(String.valueOf(value));
		}
	}
	public void setModel(final Model model) {
		this.model = model;
		revalidate();
		repaint();
	}

	public Model getModel() {
		return model;
	}

	private String addAction(Action action) {
		final String copyActionName = (String) action.getValue(Action.NAME);
		getActionMap().put(copyActionName, action);
		return copyActionName;
	}

	public String getSelectedText() {
		final Tuple<Integer, Integer> range = getSelectedRange();
		if (range != null) {
			StringBuilder sb = new StringBuilder();
			for (int i = range.first; i <= range.second; i++) {
				Strip strip = model.lines.get(i);
				if( i != range.first ) sb.append('\n');
				sb.append(strip);				
			}
			return sb.toString();
		}
		return "";
	}

	public String getSelectedHtml() {
		final Tuple<Integer, Integer> range = getSelectedRange();
		if (range != null) {
			final HtmlTransform html = new HtmlTransform();
			for (int i = range.first; i <= range.second; i++) {
				html.visitLine(model.lines.get(i));
			}
			return html.toString();
		}
		return "";
	}

	private Tuple<Integer, Integer> getSelectedRange() {
		if(startBlock != null && endBlock != null)  {
			final float y0, y1;
			if(selectionStart.y < selectionEnd.y) {
				y0 = selectionStart.y;
				y1 = selectionEnd.y;
			} else {
				y0 = selectionEnd.y;
				y1 = selectionStart.y;
			}
			return Tuple.make(lineAt(y0), lineAt(y1));
		}
		return null;
	}

	@Override
	public void invalidate() {
		updateDimensions();
		super.invalidate();
	}

	private void updateDimensions() {
		float totalHeight = 0, maxWidth = 0;
		for (Strip line : model.lines) {
			final float lineHeight = line.height();
			line.position(0, totalHeight);
			totalHeight += lineHeight;
			maxWidth = max(maxWidth, line.width());				
		}
		
		if(!model.lines.isEmpty()) {
			avgLineHeight = totalHeight / model.lines.size();
		}
		setPreferredSize(new Dimension((int)maxWidth, (int)totalHeight));
	}

	protected Block blockAt(float x, float y) {
		return model.lines.isEmpty() ? null : model.lines.get(clamp(lineAt(y))).blockAt(x);
	}

	private int lineAt(float y) {
		int index = Collections.binarySearch(model.lines, y, Y_COMPARATOR);

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
		return (int) avgLineHeight;
	}

	@Override
	public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
		return (int) (getParent().getHeight() - avgLineHeight * 3);
	}

	@Override
	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	@Override
	public boolean getScrollableTracksViewportHeight() {
		return false;
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

		// Clear background
		g.setBackground(Color.WHITE);
		g.clearRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

		float y = y1;
		for (int i = lineAt(y1); i < model.lines.size() && y <= y2; i++) {
			final Strip line = model.lines.get(i);
			line.draw(g);
			y = line.position.y;
		}
	}

	public abstract class Block<B extends RowViewer.Block> {
		protected final Point2D.Float position = new Point2D.Float(); 
		protected boolean opaque;
		private Color color = Color.BLACK;
		private Color background = Color.WHITE;

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
			final boolean selected;
			if(endBlock != null && startBlock != null) {
				// Only line selection is supported
				selected = (startBlock.position.y <= position.y && position.y <= endBlock.position.y)
						|| (endBlock.position.y <= position.y && position.y <= startBlock.position.y);
			} else {
				selected =false;
			}
					
			if (opaque || selected) {
				g.setColor(background(selected));
				g.fill(new Rectangle2D.Float(position.x, position.y, w, h));
			}
			g.setColor(color(selected));
		}
		
		protected RowViewer.Block blockAt(float x) {
			return position.x <= x && x <= position.x + width() ? this : null;
		}

		abstract float height();
		abstract float width();

		@Override
		public String toString() {
			return "";
		}
		
		abstract void visit(BlockVisitor visitor);
	}


	public abstract class Container<T extends Container> extends Block<T> {
		protected final Block block;

		protected Container(Block block) {
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
		float width() {
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

	public class Strip extends Block<RowViewer.Strip> {
		private List<Block> blocks = new ArrayList<>();
		private float lpad = 0;

		Strip() {
			opaque = true;
		}

		RowViewer.Strip add(Block... values) {
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
			final Rectangle clipBounds = g.getClipBounds();
			for (Block block : blocks) {
				if(block.position.x > clipBounds.x+clipBounds.width) break;
				block.draw(g);
			}
		}

		@Override
		float width() {
			float width = lpad;
			for (Block block : blocks) {
				width += block.width();
			}
			return width;
		}
		
		RowViewer.Strip lpad(final float lpad) {
			this.lpad = lpad;
			return this;
		}

		@Override
		protected RowViewer.Strip position(float x, float y) {
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
		void visit(BlockVisitor visitor) { visitor.visit(this); }

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

	private static final Pattern TAB_PATTERN = Pattern.compile("\t", Pattern.LITERAL);
	public class Text extends Block<RowViewer.Text> {
		private String text;
		private float vgap = 5;
		private float hgap = 5;
		
		private boolean bold;
		private boolean monospaced;
		private boolean italic;
		private boolean underline;
		private int size = 12;

		Text(String text) {
			text(text);
		}

		public void text(String text) {
			// Quick & Dirty fix for tabs
			this.text = TAB_PATTERN.matcher(text).replaceAll("    ");
		}

		public String text() {
			return this.text;
		}

		public RowViewer.Text monospaced() {
			return monospaced(true);
		}

		public RowViewer.Text monospaced(final boolean monospaced) {
			this.monospaced = monospaced;
			return this;
		}

		public RowViewer.Text bold() {
			return bold(true);
		}

		public RowViewer.Text bold(final boolean v) {
			this.bold = v;
			return this;
		}

		public RowViewer.Text size(final int size) {
			this.size = size;
			return this;
		}
		
		public RowViewer.Text italic() {
			return italic(true);
		}

		public RowViewer.Text italic(final boolean v) {
			italic = v;
			return this;
		}

		public RowViewer.Text underline() {
			return underline(true);
		}

		public RowViewer.Text underline(final boolean v) {
			underline = v;
			return this;
		}

		public RowViewer.Text hgap(float hgap) {
			this.hgap = hgap;
			return this;
		}

		private FontMetrics fontMetrics() {
			return RichTextView.this.fontMetrics(monospaced, bold, italic, underline, size);
		}

		private Font font() {
			return RichTextView.this.font(monospaced, bold, italic, underline, size);
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

		public RowViewer.Text vgap(final float vgap) {
			this.vgap = vgap;
			return this;
		}

		@Override
		void visit(BlockVisitor visitor) { visitor.visit(this); }

		@Override
		public String toString() {
			return hgap == 0?text : " " + text;
		}

		public boolean isMonospaced() {
			return monospaced;
		}

		public boolean isBold() {
			return bold;
		}

		public boolean isItalic() {
			return italic;
		}
	}

	public class HBox extends Container<RowViewer.HBox> {
		private RichTextView.Align align = RichTextView.Align.LEFT;
		private float width;

		HBox(Block block, float width) {
			super(block);
			this.width = width;
			opaque = true;
		}

		public RowViewer.HBox right() {
			align = RichTextView.Align.RIGHT;
			return this;
		}

		public RowViewer.HBox left() {
			align = RichTextView.Align.LEFT;
			return this;
		}

		public RowViewer.HBox center() {
			align = RichTextView.Align.CENTER;
			return this;
		}
		
		@Override
		protected RowViewer.HBox position(float x, float y) {
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
		float width() {
			return width;
		}

		@Override
		void visit(BlockVisitor visitor) { visitor.visit(this); }

		public Align align() {
			return align;
		}
	}

	public class Link extends Container<Link> {
		private final Block anchor;

		protected Link(final Block block, final Block anchor) {
			super(block);
			this.anchor = anchor;
		}

		@Override
		protected Block blockAt(float x) {
			final Block block = super.blockAt(x);
			return block != null ? this : null;
		}

		@Override
		void visit(BlockVisitor visitor) { visitor.visit(this); }
	}


	public class HRuler extends Block<RowViewer.HRuler> {

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

		@Override
		void visit(BlockVisitor visitor) { visitor.visit(this); }
	}

	public class Gap extends Block<Block> {
		private final int gap;

		public Gap(int gap) {
			this.gap = gap;
		}

		@Override
		float height() {
			return 0;
		}

		@Override
		float width() {
			return gap;
		}

		@Override
		void visit(BlockVisitor visitor) { visitor.visit(this); }
	}

	enum Align { LEFT, CENTER, RIGHT }
	
	private static final RichTextView.MappedComparator Y_COMPARATOR = new RichTextView.MappedComparator() {
		@Override
		protected float toFloat(Block block) {
			return block.position.y;
		}
	};

	private static final MappedComparator X_COMPARATOR = new MappedComparator() {
		@Override
		protected float toFloat(Block block) {
			return block.position.x;
		}
	};
	
	/** This class is not typesafe but allows for fast searches */
	private static abstract class MappedComparator implements Comparator<Object> {
		@Override
		public int compare(Object o1, Object o2) {
			return Float.compare(toFloat(o1), toFloat(o2));
		}

		private float toFloat(Object o) {
			return o instanceof Float ? (Float) o : toFloat((Block) o);
		}

		protected abstract float toFloat(Block block);
	}

	private Map<Integer, Font> fontCache = new HashMap<>(); 
	private Font font(boolean monospaced, boolean bold, boolean italic, boolean underline, int size) {
		final int key = (monospaced?1<<30:0)
				      | (bold      ?1<<29:0)
				      | (italic    ?1<<28:0)
				      | (underline ?1<<27:0)
				      | size;

		Font font = fontCache.get(key);
		if(font == null) {

			font = monospaced ? RichTextView.monospacedFont() : RichTextView.variableFont();
			if (bold) font = font.deriveFont(Font.BOLD);
			if (italic) font = font.deriveFont(Font.ITALIC);
			if (underline) {
				final Map<TextAttribute, Integer> attributes = new HashMap<>();
				attributes.put(TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_LOW_ONE_PIXEL);
				font = font.deriveFont(attributes);
			}
			font = font.deriveFont((float)size);
			fontCache.put(key, font);
		}
		return font;
	}

	private Map<Font, FontMetrics> fontMetricsCache = new HashMap<>();
	private FontMetrics fontMetrics(boolean monospaced, boolean bold, boolean italic, boolean underline, int size) {
		final Font font = font(monospaced, bold, italic, underline, size);
		FontMetrics fontMetrics = fontMetricsCache.get(font);
		if(fontMetrics == null) {
			fontMetrics = getGraphics().getFontMetrics(font);
			fontMetricsCache.put(font, fontMetrics);
		}
		return fontMetrics;
	}

	
	private static Font monospacedFont() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, OS_X ? "Monaco" : "Courier");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}

	private static Font variableFont() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Lucida Grande");
		attributes.put(TextAttribute.SIZE, 12);
		return Font.getFont(attributes);
	}

	private static int clamp(int i) {
		return i < 0 ? 0 : i;
	}

	private static final boolean OS_X = System.getProperty("os.name").equals("Mac OS X");
	
	public interface BlockVisitor {
		void visit(Text text);
		void visit(Gap gap);
		void visit(Strip strip);
		void visit(HBox hBox);
		void visit(Link link);
		void visit(HRuler hRuler);
	}
}
