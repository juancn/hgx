package codng.hgx.ui;

import codng.hgx.Cache;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
import javax.swing.UIManager;
import javax.swing.text.TextAction;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.datatransfer.DataFlavor;
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Math.max;

public class RichTextView extends JComponent implements Scrollable {
	private final List<Strip> lines = new ArrayList<>();
	private final List<Strip> build = new ArrayList<>();
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
		};
		addMouseListener(mouseAdapter);
		addMouseMotionListener(mouseAdapter);

		initCopy();
	}

	private void initCopy() {
		setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(JComponent c) {
				return COPY;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new Transferable() {
					private final DataFlavor[] dataFlavors = {
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
						return serializeText(getSelectedText());
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
				if (!lines.isEmpty()) {
					final Strip lastLine = lines.get(lines.size() - 1);
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
		lines.clear();
		startBlock = null;
		endBlock = null;
	}
	
	public void finishBuild() {
		synchronized (build) {
			clear();
			lines.addAll(build);
			clearBuild();
		}
	}

	public void clearBuild() {
		synchronized (build) {
			build.clear();
		}
	}

	private String addAction(Action action) {
		final String copyActionName = (String) action.getValue(Action.NAME);
		getActionMap().put(copyActionName, action);
		return copyActionName;
	}

	public String getSelectedText() {
		if(startBlock != null && endBlock != null)  {
			final float y0, y1;
			if(selectionStart.y < selectionEnd.y) {
				y0 = selectionStart.y;
				y1 = selectionEnd.y;
			} else {
				y0 = selectionEnd.y;
				y1 = selectionStart.y;
			}
			
			final int line0 = lineAt(y0);
			final int line1 = lineAt(y1);

			StringBuilder sb = new StringBuilder();
			for (int i = line0; i <= line1; i++) {
				Strip strip = lines.get(i);
				if( i != line0 ) sb.append('\n');
				sb.append(strip);				
			}
			return sb.toString();
		}
		return "";
	}

	@Override
	public void invalidate() {
		updateDimensions();
		super.invalidate();
	}

	private void updateDimensions() {
		float totalHeight = 0, maxWidth = 0;
		for (Strip line : lines) {
			final float lineHeight = line.height();
			line.position(0, totalHeight);
			totalHeight += lineHeight;
			maxWidth = max(maxWidth, line.width());				
		}
		
		if(!lines.isEmpty()) {
			avgLineHeight = totalHeight / lines.size();
		}
		setPreferredSize(new Dimension((int)maxWidth, (int)totalHeight));
	}

	protected Block blockAt(float x, float y) {
		return lines.get(clamp(lineAt(y))).blockAt(x);
	}

	private int lineAt(float y) {
		int index = Collections.binarySearch(lines, y, Y_COMPARATOR);

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

	protected Strip numbered(int oldStart, int newStart, Block block) {
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
		final Strip line = strip().lpad(25);
		synchronized (build) {
			build.add(line);
		}
		return line;
	}

	Strip strip() {
		return new Strip();
	}

	protected void hr() {
		synchronized (build) {
			build.add(strip().add(new HRuler(getParent().getWidth())));
		}
	}

	protected void header(String label, Text value) {
		synchronized (build) {
			build.add(strip().add(align(text(label).rgb(127, 127, 127).bold(), 100).right(), value));
		}
	}

	protected void header(String label, Object value) {
		header(label, text(value));
	}

	protected HBox align(Block block, float width) {
		return new HBox(block, width);
	}

	protected Text text(Object value) {
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

		// Clear background
		g.setBackground(Color.WHITE);
		g.clearRect(clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height);

		float y = y1;
		for (int i = lineAt(y1); i < lines.size() && y <= y2; i++) {
			final Strip line = lines.get(i);
			line.draw(g);
			y = line.position.y;
		}
	}

	public abstract class Block<B extends RowViewer.Block> {
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
			final boolean selected;
			if(endBlock != null && startBlock != null) {
				selected = startBlock == this 
						|| endBlock == this 
						|| (startBlock.position.y < position.y && position.y < endBlock.position.y)
						|| (endBlock.position.y < position.y && position.y < startBlock.position.y);
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
		public String toString() {
			final StringBuilder sb = new StringBuilder();
			for (int i = 0; i < blocks.size(); i++) {
				Block block = blocks.get(i);
				sb.append(block);
			}
			return sb.toString(); 
		}
	}

	public class Text extends Block<RowViewer.Text> {
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

		public RowViewer.Text bold() {
			bold = true;
			return this;
		}

		public RowViewer.Text size(final int size) {
			this.size = size;
			return this;
		}
		
		public RowViewer.Text italic() {
			italic = true;
			return this;
		}

		public RowViewer.Text hgap(float hgap) {
			this.hgap = hgap;
			return this;
		}

		private FontMetrics fontMetrics() {
			return getGraphics().getFontMetrics(font());
		}

		private Font font() {
			Font font = monospaced ? RichTextView.monospacedFont() : RichTextView.variableFont();
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

		public RowViewer.Text monospaced() {
			monospaced = true;
			return this;
		}

		public RowViewer.Text vgap(final float vgap) {
			this.vgap = vgap;
			return this;
		}

		@Override
		public String toString() {
			return hgap == 0?text : " " + text;
		}
	}

	public class HBox extends Block<RowViewer.HBox> {
		private RichTextView.Align align = RichTextView.Align.LEFT;
		private Block block;
		private float width;

		HBox(Block block, float width) {
			this.block = block;
			this.width = width;
			opaque = true;
		}

		RowViewer.HBox right() {
			align = RichTextView.Align.RIGHT;
			return this;
		}

		RowViewer.HBox left() {
			align = RichTextView.Align.LEFT;
			return this;
		}

		RowViewer.HBox center() {
			align = RichTextView.Align.CENTER;
			return this;
		}
		
		@Override
		protected void draw(Graphics2D g) {
			super.draw(g);
			block.draw(g);
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
		float height() {
			return block.height();
		}

		@Override
		float width() {
			return width;
		}

		@Override
		public String toString() {
			return block.toString();
		}
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

	private static Font monospacedFont() {
		Map<TextAttribute, Object> attributes = new HashMap<>();
		attributes.put(TextAttribute.FAMILY, "Monaco");
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
}
