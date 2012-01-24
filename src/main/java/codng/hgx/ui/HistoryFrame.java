package codng.hgx.ui;

import codng.hgx.Cell;
import codng.hgx.ChangeSet;
import codng.hgx.Command;
import codng.hgx.History;
import codng.hgx.Row;

import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.Timer;
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
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

public class HistoryFrame 
		extends JFrame 
{

	private static final String RULER = "<hr style=\"border-top-width: 0px; border-right-width: 0px; border-bottom-width: 0px; border-left-width: 0px; border-style: initial; border-color: initial; height: 1px; margin-top: 0px; margin-right: 8px; margin-bottom: 0px; margin-left: 8px; background-color: rgb(222, 222, 222); clear: both; font-family: 'Lucida Grande';\">";
	private static final File CACHE_DIR = new File(System.getProperty("user.home"), ".hgx");

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
					final Row row = (Row)historyTableModel.getValueAt(historyTable.getSelectedRow(), 0);
					detail.setText(buildDetail(row));
					detail.setCaretPosition(0);
				}
			}
		});
	}

	private String buildDetail(Row row) {
		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw, true);
		pw.print("<html>");
		pw.print("<body style=\"word-wrap: break-word;\">");
		pw.print("<table id=\"commit_header\" style=\"font-size: 11px; font-family: 'Lucida Grande';\">");
		pw.print("<tbody>");
		pw.printf(HEADER_ROW, "SHA:", row.changeSet.id);
		pw.printf(HEADER_ROW, "Author:", row.changeSet.user);
		pw.printf(HEADER_ROW, "Date:", row.changeSet.date);
		pw.printf(HEADER_ROW, "Summary:", "<b>" + htmlEscape(row.changeSet.summary) + "</b>");
		pw.printf(HEADER_ROW, "Parent:", row.changeSet.parents);
		pw.print("</tbody>");
		pw.print("</table>");
		pw.print(RULER);

		try {
			colorize(pw, loadDiff(row));
		} catch (IOException e) {
			e.printStackTrace();
			pw.printf("<pre>%s</pre>", e.getMessage());
		}

		pw.print("</body>");
		pw.print("</html>");
		pw.close();
		return sw.toString();
	}

	private String loadDiff(Row row) throws IOException {
		String key = row.changeSet.parents.get(0).hash + "-" + row.changeSet.id.hash;
		File file = new File(CACHE_DIR, key);
		final String diff;
		if(file.exists()) {
			diff = read(file);
		} else {
			diff = Command.executeSimple("hg", "diff", "-r", row.changeSet.parents.get(0).hash, "-r", row.changeSet.id.hash);
			write(diff, file);
		}
		return diff;
	}

	private void write(String diff, File file) throws IOException {
		final File parentFile = file.getParentFile();
		if(!parentFile.exists()) {
			parentFile.mkdirs();
		}
		
		final File temp = File.createTempFile("diff", "tmp", parentFile);
		final FileOutputStream fos = new FileOutputStream(temp);
		final OutputStreamWriter out = new OutputStreamWriter(new BufferedOutputStream(fos), "UTF-8");
		out.write(diff);
		out.close();;
		temp.renameTo(file);
	}

	private String read(File file) throws IOException {
		final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file));
		final Reader in = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		final StringWriter out = new StringWriter();
		final char[] buffer = new char[512];
		int read;
		while ((read = in.read(buffer)) != -1) {
			out.write(buffer, 0, read);
		}
		in.close();
		return out.toString();
	}

	private void colorize(PrintWriter pw,  String diff) {
		final StringReader sr = new StringReader(diff);
		final BufferedReader br = new BufferedReader(sr);
		try {
			pw.println("<pre>");
			for(String rawLine = br.readLine(); rawLine != null; rawLine = br.readLine())  {
				final String line = htmlEscape(rawLine);

				if(rawLine.startsWith("+++")) {
					pw.println(line);
				} else if(rawLine.startsWith("---")) {
					pw.println(line);
				} else if(rawLine.startsWith("@@")) {
					pw.println(line.replace("@@", "<span style=\"color: rgb(255, 0, 0);\">@@</span>"));
				} else if(rawLine.startsWith("-")) {
					pw.printf("<span style=\"background: rgb(255,144,144);\">%s</span>\n", line);
				} else if(rawLine.startsWith("+")) {
					pw.printf("<span style=\"background: rgb(144,255,144);\">%s</span>\n", line);
				} else {
					pw.println(line);
				}
			}
			pw.println("</pre>");
		} catch (IOException e) {
			throw new Error("This shouldn't happen!");
		}
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
