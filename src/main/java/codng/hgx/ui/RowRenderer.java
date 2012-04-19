package codng.hgx.ui;

import codng.hgx.Cell;
import codng.hgx.Row;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

class RowRenderer implements TableCellRenderer {
	private static final BasicStroke THICK = new BasicStroke(2.0f);
	private static final MessageDigest DIGEST = createDigest();

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

				final Row previousRow = rowIndex > 0 ? (Row) table.getModel().getValueAt(rowIndex - 1, columnIndex) : null;
				int bullet = drawLines(g, previousRow, cellSize);
				drawBullet(g, cellSize, 0, bullet);
				drawSummary(g, cellSize, 0, 0, previousRow, row);
			}

			private int drawLines(Graphics2D g, Row previousRow, int cellSize) {
				final TableModel tableModel = table.getModel();
				final boolean hasNext = rowIndex + 1 < tableModel.getRowCount();
				// First draw the highlights
				final int bullet = drawLines(g, cellSize, 0, 0, previousRow, row, true);
				if (hasNext) {
					drawLines(g, cellSize, 0, getHeight(), row, (Row) tableModel.getValueAt(rowIndex + 1, columnIndex), true);
				}

				// then the solid line
				drawLines(g, cellSize, 0, 0, previousRow, row, false);
				if (hasNext) {
					drawLines(g, cellSize, 0, getHeight(), row, (Row) tableModel.getValueAt(rowIndex + 1, columnIndex), false);
				}
				return bullet;
			}

			private int drawLines(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow, boolean highlight) {
				final int halfCell = cellSize / 2;
				final int y0 = yoff + getHeight() / 2;
				final int y1 = yoff - getHeight() / 2;

				int bulletOff = -1;
				final Color c = g.getColor();
				final Stroke stroke = g.getStroke();
				for (int j = 0; j < currentRow.cells.size(); j++) {
					final int x = cellOffset(halfCell, xoff, j);
					final Cell cell = currentRow.cells.get(j);
					if (cell.id.equals(currentRow.changeSet.id)) {
						assert bulletOff == -1;
						bulletOff = x;
					}
					if (previousRow != null) {
						for (Cell child : cell.children) {
							final int prev = previousRow.cellIndex(child);
							if (prev != -1) {
								if (highlight) {
									g.setColor(branchColor(child.branch));
									g.setStroke(THICK);
								}
								g.drawLine(x + halfCell, y0, cellOffset(halfCell, xoff, prev) + halfCell, y1);
							}
						}
					}
				}
				g.setColor(c);
				g.setStroke(stroke);
				return bulletOff;
			}

			private void drawSummary(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
				final int halfCell = cellSize / 2;

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
					g.setColor(Color.BLACK);
					g.drawString(branch, xbase + 3, yoff + halfCell + g.getFont().getSize() / 2);
					g.setColor(color);
				}
				g.drawString(currentRow.changeSet.summary, xbase + xlabel, yoff + halfCell + g.getFont().getSize() / 2);
			}

			private Color branchColor(String branch) {
				final byte[] digest = DIGEST.digest(branch.getBytes());
				final float a = digest[0]*0.9f;
				final float b = digest[1]*0.6f;
				float[] rgb = CIELab.getInstance().toRGB(new float[]{80f, a, b});
				return new Color(rgb[0], rgb[1], rgb[2]);
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

	private static MessageDigest createDigest(){
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new Error(e);
		}
	}
}
