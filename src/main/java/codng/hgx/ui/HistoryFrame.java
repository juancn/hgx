package codng.hgx.ui;

import codng.hgx.Cell;
import codng.hgx.ChangeSet;
import codng.hgx.Hg;
import codng.hgx.History;
import codng.hgx.Row;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.FileInputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HistoryFrame 
		extends JFrame 
{
	private String branch;
	private JTable historyTable;
	private JSplitPane split;
	private RowViewer detail;

	public HistoryFrame(String branch, Iterator<Row> historyGen) throws HeadlessException {
		super(branch);
		this.branch = branch;

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

//		final JPanel topPanel = new JPanel();
//		createBranchCombo(topPanel);
//		getContentPane().add(topPanel, BorderLayout.NORTH);
		getContentPane().add(split, BorderLayout.CENTER);

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
					detail.setRow((Row) historyTableModel.getValueAt(historyTable.getSelectedRow(), 0));
					detail.scrollRectToVisible(new Rectangle());
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

	private void createBranchCombo(JPanel topPanel) {
		final JComboBox<String> combo = new JComboBox<>();
		combo.setEditable(true);
		combo.setUI(new BasicComboBoxUI() {
			@Override
			protected JButton createArrowButton() {
				final JButton jButton = new JButton(new ImageIcon(getClass().getResource("search.png")));
				jButton.setBorderPainted(false);
				jButton.setBackground(Color.WHITE);
				jButton.setForeground(Color.WHITE);
				jButton.setContentAreaFilled(true);
				jButton.setOpaque(true);
				return jButton;
			}

			@Override
			protected ListCellRenderer createRenderer() {
				final ListCellRenderer renderer = super.createRenderer();
				return new ListCellRenderer() {
					@Override
					public Component getListCellRendererComponent(JList jList, Object value, int index, boolean isSelected, boolean cellHasFocus) {
						final Component listCellRendererComponent = renderer.getListCellRendererComponent(jList, value, index, isSelected, cellHasFocus);
						if(!isSelected) listCellRendererComponent.setBackground(Color.WHITE);						
						return listCellRendererComponent;
					}
				};
			}
		});

		final Dimension preferredSize = combo.getPreferredSize();
		preferredSize.width = 200;
		combo.setPreferredSize(preferredSize);
		final JTextField editorComponent = (JTextField) combo.getEditor().getEditorComponent();
		editorComponent.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent e) {
				final String text = editorComponent.getText();
				for (int i = 0; i < combo.getItemCount(); i++) {
					String item =  combo.getItemAt(i);
					if(item.startsWith(text)) {
						combo.setSelectedIndex(i);
						break;
					}
				}
				combo.showPopup();
			}
		});

		topPanel.add(new JLabel("Branch:"));
		topPanel.add(combo);
		combo.addItem(branch);
		new SwingWorker<List<String>, Void>() {
			@Override
			protected List<String> doInBackground() throws Exception {
				final ArrayList<String> result = new ArrayList<>();
				for (Hg.Branch branch : Hg.branches()) {
					result.add(branch.name);
				}
				Collections.sort(result);
				return result;
			}

			@Override
			protected void done() {
				try {
					final List<String> branches = get();
					for (String b : branches) {
						combo.addItem(b);
					}
				} catch (InterruptedException | ExecutionException e) {
					e.printStackTrace();
				}
			}
		}.execute();
	}

	private void doShow() throws InvocationTargetException, InterruptedException {
		// Pick some pleasing proportions 
		final double golden = 1.61803399;
		setSize((int) (900*golden),900);
		setVisible(true);
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				setLocationRelativeTo(null);
				historyTable.getColumnModel().getColumn(0).setPreferredWidth(900);
				split.setDividerLocation(1 - (1 / golden));
				detail.setPreferredSize(getSize());
				// Select the first row
				historyTable.getSelectionModel().setSelectionInterval(0, 0);
			}
		});
	}


	public static void main(String[] args) throws Exception {
		final String branch;
		final List<ChangeSet> changeSets;
		if (args.length == 1 &&  "--debug".equals(args[0])) {
			branch = "Debug";
			changeSets = ChangeSet.filterBranch("case16146",ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log")), false);
			ChangeSet.linkParents(changeSets);
		} else {
			branch = Hg.branch();
			final boolean branchOnly = args.length == 1 && "--branch-only".equals(args[0]);
			changeSets = ChangeSet.filterBranch(branch, ChangeSet.loadFromCurrentDirectory(), branchOnly);
		}
		final History tb = new History(changeSets);
		final HistoryFrame blah = new HistoryFrame(branch, tb.iterator());
		blah.doShow();
	}
}
