package codng.hgx.ui;

import codng.hgx.Cache;
import codng.hgx.Cell;
import codng.hgx.ChangeSet;
import codng.hgx.Hg;
import codng.hgx.History;
import codng.hgx.Row;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.FileInputStream;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class HistoryFrame 
		extends JFrame 
{
	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
	private JTable historyTable;
	private JSplitPane split;
	private RowViewer detail;

	public HistoryFrame(String title, Iterator<Row> historyGen) throws HeadlessException {
		super(title);

		final HistoryTableModel historyTableModel = new HistoryTableModel(historyGen);
		historyTable = new JTable(historyTableModel);
		
		historyTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, boolean hasFocus, final int rowIndex, final int columnIndex) {
				final Row row = (Row) value;
				final JComponent renderer = new JComponent() {
					@Override
					public void paint(Graphics g2) {
						final int cellSize = 14;

						final Graphics2D g = (Graphics2D) g2;
						if (isSelected) {
							g.setBackground(table.getSelectionBackground());
							g.setColor(table.getSelectionForeground());
						} else {
							g.setBackground(Color.WHITE);
							g.setColor(Color.BLACK);
						}
						g.clearRect(0, 0, getWidth(), getHeight());
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

						final Row previousRow = rowIndex > 0 ? (Row) historyTableModel.getValueAt(rowIndex - 1, columnIndex) : null;
						int bullet = drawLines(g, cellSize, 0, 0, previousRow, row);
						if (rowIndex + 1 < historyTableModel.getRowCount()) {
							drawLines(g, cellSize, 0, getHeight(), row, (Row) historyTableModel.getValueAt(rowIndex + 1, columnIndex));
						}
						drawBullet(g, cellSize, 0, bullet);
						drawSummary(g, cellSize, 0, 0, previousRow, row);
					}

					private int drawLines(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
						final int halfCell = cellSize / 2;
						int bulletOff = -1;
						final Color c = g.getColor();
						g.setColor(new Color(0, 200, 0));
						for (int j = 0; j < currentRow.cells.size(); j++) {
							int x = cellOffset(halfCell, xoff, j);
							Cell cell = currentRow.cells.get(j);
							if (cell.id.equals(currentRow.changeSet.id)) {
								assert bulletOff == -1;
								bulletOff = x;
							}

							if (previousRow != null) {
								for (Cell child : cell.children) {
									final int prev = previousRow.cellIndex(child);
									if (prev != -1) {
										g.drawLine(x + halfCell, yoff + getHeight() / 2, cellOffset(halfCell, xoff, prev) + halfCell, yoff - getHeight() / 2);
									}
								}
							}
						}
						g.setColor(c);
						return bulletOff;
					}

					private void drawSummary(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
						final int halfCell = cellSize / 2;
						if (isSelected)
							g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

						final int xbase = cellOffset(halfCell, xoff, currentRow.cells.size()) + cellSize;
						int xlabel = 0;
						final String branch = currentRow.changeSet.branch;
						if(previousRow == null || !previousRow.changeSet.branch.equals(branch)) {
							final Color color = g.getColor();
							final int width = g.getFontMetrics().stringWidth(branch);
							xlabel = width + 10;
							final Color labelColor = branchColor(branch);
							g.setColor(labelColor);
							g.fillRoundRect(xbase, 1, width + 6, getHeight() - 2, 5, 5);
							g.setColor(labelColor.darker());
							g.drawRoundRect(xbase, 1, width + 6, getHeight() - 2, 5, 5);
							g.setColor(color);
							g.drawString(branch, xbase + 3, yoff + halfCell + g.getFont().getSize() / 2);
						}
						g.drawString(currentRow.changeSet.summary, xbase + xlabel, yoff + halfCell + g.getFont().getSize() / 2);
					}

					private Color branchColor(String branch) {
						return new Color(Color.HSBtoRGB((Math.abs(branch.hashCode())%30)/30f, 0.3f, 1f));
					}

					private void drawBullet(Graphics2D g, int cellSize, int yoff, int xoff) {
						final int halfCell = cellSize / 2;
						final int quarterCell = halfCell / 2;
						g.fillOval(xoff + quarterCell + 1, yoff + getHeight() / 2 - quarterCell, halfCell, halfCell);
						final Color color = g.getColor();
						g.setColor(g.getBackground());
						g.fillOval(xoff + quarterCell + 2, yoff + getHeight() / 2 - quarterCell + 1, halfCell - 2, halfCell - 2);
						g.setColor(color);
					}

					private int cellOffset(int halfCell, int xoff, int column) {
						return column * (halfCell) + xoff;
					}
				};
				return renderer;
			}
		});
		historyTable.setRowMargin(0);
		historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tableScrollPane = new JScrollPane(historyTable);


		detail = new RowViewer();
		
		split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(tableScrollPane);
		final JScrollPane detailScrollPane = new JScrollPane(detail);
		split.setBottomComponent(detailScrollPane);
		getContentPane().add(split);

		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		historyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			public ScheduledFuture<?> future;

			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					if (future != null) {
						future.cancel(false);
					}
					final AtomicReference<Row> rowRef = new AtomicReference<>((Row) historyTableModel.getValueAt(historyTable.getSelectedRow(), 0));

					// I should move all this to the RowViewer component
					future = scheduler.schedule(new Runnable() {
						@Override
						public void run() {
							try {
								SwingUtilities.invokeAndWait(new Runnable() {
									@Override
									public void run() {
										detail.setRow(rowRef.get());
										detail.scrollRectToVisible(new Rectangle());
									}
								});
							} catch (Exception e) {
								// Safe to ignore, but log
								e.printStackTrace();
							}

						}
					}, 100, TimeUnit.MILLISECONDS);

				}
			}
		});
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// Just die
				System.exit(0);
			}
		});
	}

	private void initSize() {
		// Pick some pleasing proportions 
		final double golden = 1.61803399;
		setSize((int) (900*golden),900);
		setVisible(true);
		setLocationRelativeTo(null);

		historyTable.getColumnModel().getColumn(0).setPreferredWidth(900);
		split.setDividerLocation(1-(1/golden));
		detail.setPreferredSize(getSize());
		// Select the first row
		historyTable.getSelectionModel().setSelectionInterval(0, 0);
	}


	public static void main(String[] args) throws Exception {
		
		final List<ChangeSet> changeSets;
		if (args.length == 1 &&  "--debug".equals(args[0])) { 
			changeSets = ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log"));
			ChangeSet.linkParents(changeSets);
		} else {
			changeSets = ChangeSet.filterBranch(Hg.branch(), ChangeSet.loadFromCurrentDirectory());
		}
		final History tb = new History(changeSets);
		final HistoryFrame blah = new HistoryFrame("blah", tb.iterator());
		blah.initSize();
		
	}
}
