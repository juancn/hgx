package codng.hgx.ui;

import codng.hgx.Cell;
import codng.hgx.ChangeSet;
import codng.hgx.Row;
import codng.hgx.TreeBuilder;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.FileInputStream;
import java.util.Iterator;

public class MainFrame 
		extends JFrame 
{
	public MainFrame(String title, Iterator<Row> historyGen) throws HeadlessException {
		super(title);

		final HistoryTableModel historyTableModel = new HistoryTableModel(historyGen);
		final JTable historyTable = new JTable(historyTableModel);
		
		historyTable.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, boolean hasFocus, final int rowIndex, final int columnIndex) {
				final Row row = (Row) value;
				final JComponent renderer = new JComponent() {
					@Override
					public void paint(Graphics g2) {
						final int cellSize = table.getRowHeight() + table.getRowMargin();

						final Graphics2D g = (Graphics2D) g2;
						if (isSelected) {
							// TODO: fetch actual colors
							g.setBackground(Color.BLUE);
							g.setColor(Color.WHITE);
						} else {
							g.setBackground(Color.WHITE);
							g.setColor(Color.BLACK);
						}
						g.clearRect(0,0,getWidth(), getHeight());
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

						drawRow(g, cellSize, 0, 0, rowIndex > 0 ? (Row) historyTableModel.getValueAt(rowIndex - 1, columnIndex) : null, row);
						if (rowIndex + 1 < historyTableModel.getRowCount()) {
							drawRow(g, cellSize, 0, getHeight(), row, (Row) historyTableModel.getValueAt(rowIndex + 1, columnIndex));
						}
					}

					private void drawRow(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
						for (int j = 0; j < currentRow.cells.size(); j++) {
							int x = j * cellSize + xoff;
							Cell cell = currentRow.cells.get(j);
							if (cell.id.equals(currentRow.changeSet.id)) {
								g.fillOval(x + cellSize/4, yoff + getHeight()/2 - cellSize / 4, cellSize / 2, cellSize / 2);
							}

							if (previousRow != null) {
								for (Cell child : cell.children) {
									final int prev = previousRow.cellIndex(child);
									if (prev != -1) {
										g.drawLine(x + cellSize / 2, yoff + getHeight()/2, (prev * cellSize + xoff) + cellSize / 2, yoff - getHeight()/2);
									}
								}
							}
						}
						g.drawString(currentRow.changeSet.summary, currentRow.cells.size() * cellSize + xoff + cellSize, yoff + cellSize / 2 + g.getFont().getSize() / 2);
					}
				};
				return renderer;
			}
		});
		historyTable.setRowMargin(0);
		JScrollPane tableScrollPane = new JScrollPane(historyTable);
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(tableScrollPane);
		split.setBottomComponent(new JLabel());
		getContentPane().add(split);
		historyTable.getColumnModel().getColumn(0).setPreferredWidth(800);
	}

	public static void main(String[] args) throws Exception {
		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log")));
//		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log")));

		final MainFrame blah = new MainFrame("blah", tb.iterator());
		blah.setSize(1024,800);
		blah.setVisible(true);
		
	}
}
