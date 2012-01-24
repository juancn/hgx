package codng.hgx.ui;

import codng.hgx.Cell;
import codng.hgx.ChangeSet;
import codng.hgx.Command;
import codng.hgx.Row;
import codng.hgx.History;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.RenderingHints;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Formatter;
import java.util.Iterator;
import java.util.List;

public class HistoryFrame 
		extends JFrame 
{

	private static final String RULER = "<hr style=\"border-top-width: 0px; border-right-width: 0px; border-bottom-width: 0px; border-left-width: 0px; border-style: initial; border-color: initial; height: 1px; margin-top: 0px; margin-right: 8px; margin-bottom: 0px; margin-left: 8px; background-color: rgb(222, 222, 222); clear: both; font-family: 'Lucida Grande';\">";

	public HistoryFrame(String title, Iterator<Row> historyGen) throws HeadlessException {
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
						final int cellSize = 14;

						final Graphics2D g = (Graphics2D) g2;
						if (isSelected) {
							g.setBackground(table.getSelectionBackground());
							g.setColor(table.getSelectionForeground());
						} else {
							g.setBackground(Color.WHITE);
							g.setColor(Color.BLACK);
						}
						g.clearRect(0,0,getWidth(), getHeight());
						g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

						int bullet = drawLines(g, cellSize, 0, 0, rowIndex > 0 ? (Row) historyTableModel.getValueAt(rowIndex - 1, columnIndex) : null, row);
						if (rowIndex + 1 < historyTableModel.getRowCount()) {
							drawLines(g, cellSize, 0, getHeight(), row, (Row) historyTableModel.getValueAt(rowIndex + 1, columnIndex));
						}
						drawBullet(g, cellSize, 0, bullet);
						drawSummary(g, cellSize, 0, 0, row);
					}

					private int drawLines(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
						final int halfCell = cellSize / 2;
						int bulletOff = -1;
						
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
										g.drawLine(x + halfCell, yoff + getHeight()/2, cellOffset(halfCell, xoff, prev) + halfCell, yoff - getHeight()/2);
									}
								}
							}
						}
						return bulletOff;
					}

					private void drawSummary(Graphics2D g, int cellSize, int xoff, int yoff, Row currentRow) {
						final int halfCell = cellSize / 2;
						if(isSelected) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
						g.drawString(currentRow.changeSet.summary, cellOffset(halfCell, xoff, currentRow.cells.size()) + cellSize, yoff + halfCell + g.getFont().getSize() / 2);
					}

					private void drawBullet(Graphics2D g, int cellSize, int yoff, int xoff) {
						final int halfCell = cellSize / 2;
						final int quarterCell = halfCell / 2;
						g.fillOval(xoff + quarterCell + 1, yoff + getHeight()/2 - quarterCell, halfCell, halfCell);
						final Color color = g.getColor();
						g.setColor(g.getBackground());
						g.fillOval(xoff + quarterCell + 2, yoff + getHeight()/2 - quarterCell + 1, halfCell - 2, halfCell - 2);
						g.setColor(color);
					}

					private int cellOffset(int halfCell, int xoff, int column) {
						return column * (halfCell ) + xoff;
					}
				};
				return renderer;
			}
		});
		historyTable.setRowMargin(0);
		historyTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane tableScrollPane = new JScrollPane(historyTable);
		final JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(tableScrollPane);
		
		final JEditorPane detail = new JEditorPane();
		detail.setContentType("text/html");
		detail.setEditable(false);
		
		split.setBottomComponent(new JScrollPane(detail));
		getContentPane().add(split);
		historyTable.getColumnModel().getColumn(0).setPreferredWidth(800);
		
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				System.exit(0);
			}
		});
		
		historyTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent e) {
				if (!e.getValueIsAdjusting()) {
					final int firstIndex = e.getFirstIndex();
					final Row row = (Row)historyTableModel.getValueAt(firstIndex, 0);
					detail.setText(buildDetail(row));
				}
			}
		});
	}

	private String buildDetail(Row row) {
		final Formatter fmt = new Formatter();
		fmt.format("<html>");
		fmt.format("<body style=\"word-wrap: break-word;\">");
		fmt.format("<table id=\"commit_header\" style=\"font-size: 11px; font-family: 'Lucida Grande';\">");
		fmt.format("<tbody>");
		fmt.format(HEADER_ROW, "SHA:", row.changeSet.id);
		fmt.format(HEADER_ROW, "Author:", row.changeSet.user);
		fmt.format(HEADER_ROW, "Date:", row.changeSet.date);
		fmt.format(HEADER_ROW, "Summary:", "<b>" + htmlEscape(row.changeSet.summary) + "</b>");
		fmt.format(HEADER_ROW, "Parent:", row.changeSet.parents);
		fmt.format("</tbody>");
		fmt.format("</table>");
		fmt.format(RULER);

		String diff;
		try {
			diff = htmlEscape(Command.executeSimple("hg", "diff", "-r", row.changeSet.id.hash, "-r", row.changeSet.parents.get(0).hash));
		} catch (IOException e) {
			e.printStackTrace();
			diff = e.toString();
		}

		fmt.format("<pre>%s</pre>", diff);
		fmt.format("</body>");
		fmt.format("</html>");
		return fmt.toString();
	}

	private String htmlEscape(String s) {
		return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	public static void main(String[] args) throws Exception {
		
		final List<ChangeSet> changeSets;
		if (args.length == 1 &&  "--debug".equals(args[0])) { 
			changeSets = ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log"));
		} else {
			changeSets = ChangeSet.loadFromCurrentDirectory();
		}
		final History tb = new History(changeSets);
//		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log")));

		final HistoryFrame blah = new HistoryFrame("blah", tb.iterator());
		blah.setSize(1024,800);
		blah.setVisible(true);
		
	}
	
	private static final String HEADER_ROW = "<tr>\n" +
			"<td class=\"property_name\" style=\"width: 6em; color: rgb(127, 127, 127); text-align: right; font-weight: bold; padding-left: 5px;\">\n" +
			"%s\n" +
			"</td>\n" +
			"<td id=\"commitID\" style=\"padding-left: 5px;\">\n" +
			"%s\n" +
			"</td>\n" +
			"</tr>";

	
}
