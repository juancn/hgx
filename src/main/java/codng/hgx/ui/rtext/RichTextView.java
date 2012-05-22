package codng.hgx.ui.rtext;

import codng.hgx.ui.rtext.HtmlTransform;
import codng.util.Tuple;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.Scrollable;
import javax.swing.TransferHandler;
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
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.Math.max;

/**
 * JComponent that renders colorized text.
 * This component can render huge amounts of text very quickly.
 * Supports line selection and plain text and HTML copying.
 */
public class RichTextView extends JComponent implements Scrollable {
	private RichTextViewModel<? extends RichTextView> model = new RichTextViewModel<>(this);
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
						link.onClick();
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

		setTransferHandler(new TransferHandler() {
			@Override
			public int getSourceActions(JComponent c) {
				return COPY;
			}

			@Override
			protected Transferable createTransferable(JComponent c) {
				return new Transferable() {
					private final DataFlavor[] dataFlavors = {
							HTML_DATA_FLAVOR,
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

	public void setModel(final RichTextViewModel<? extends RichTextView> model) {
		this.model = model;
		revalidate();
		repaint();
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


	static final Pattern TAB_PATTERN = Pattern.compile("\t", Pattern.LITERAL);


	private static final RichTextView.MappedComparator Y_COMPARATOR = new RichTextView.MappedComparator() {
		@Override
		protected float toFloat(Block block) {
			return block.position.y;
		}
	};

	static final MappedComparator X_COMPARATOR = new MappedComparator() {
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
	Font font(boolean monospaced, boolean bold, boolean italic, boolean underline, int size) {
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
	FontMetrics fontMetrics(boolean monospaced, boolean bold, boolean italic, boolean underline, int size) {
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
	private static final DataFlavor HTML_DATA_FLAVOR = new DataFlavor("text/html", "Html");
	static {
		if (OS_X) {
			// Java for OSX doesn't seem to have a native registered for HTML
			final SystemFlavorMap flavorMap = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
			flavorMap.addFlavorForUnencodedNative("public.html", HTML_DATA_FLAVOR);
			flavorMap.addUnencodedNativeForFlavor(HTML_DATA_FLAVOR, "public.html");
		}
	}

	public interface BlockVisitor {
		void visit(Text text);
		void visit(Gap gap);
		void visit(Strip strip);
		void visit(HBox hBox);
		void visit(Link link);
		void visit(HRuler hRuler);
	}
}
