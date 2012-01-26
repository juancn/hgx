import codng.hgx.Cache;
						final Row previousRow = rowIndex > 0 ? (Row) historyTableModel.getValueAt(rowIndex - 1, columnIndex) : null;
						int bullet = drawLines(g, cellSize, 0, 0, previousRow, row);
						drawSummary(g, cellSize, 0, 0, previousRow, row);
						final Color c = g.getColor();
						g.setColor(new Color(0, 200, 0));
						g.setColor(c);
					private void drawSummary(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {

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
		pw.printf(HEADER_ROW, "Branch:", "<b>" + Colorizer.htmlEscape(row.changeSet.branch) + "</b>");
			colorize(pw, Cache.loadDiff(row));
					colorizer.reset();