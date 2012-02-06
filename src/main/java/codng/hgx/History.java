package codng.hgx;

import codng.util.NoRemoveIterator;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.Iterator;
import java.util.List;

public class History 
		implements Iterable<Row>
{
	private final Iterable<ChangeSet> changeSets;

	public History(Iterable<ChangeSet> changeSets) {
		this.changeSets = changeSets;
	}

	@Override
	public Iterator<Row> iterator() {
		return new NoRemoveIterator<Row>() {
			final Iterator<ChangeSet> chIt = changeSets.iterator();
			@Override
			protected Row advance() {
				final Row result;
				if(chIt.hasNext()) {
					if(element == null) {
						result = new Row(chIt.next());
					} else {
						result = element.next(chIt.next());
					}
				} else {
					result = finished();
				}
				return result;
			}
		};
	}

	public static void main(String[] args) throws IOException, ParseException {
//		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log")));
		final List<ChangeSet> changeSets = ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log"));
		ChangeSet.linkParents(changeSets);
		final History tb = new History(changeSets);

		Frame f = new Frame("test") {
			@Override
			public void paint(Graphics g1) {
				final Graphics2D g = (Graphics2D) g1;
				final int cellSize = 16;
				final int yoff = 30;
				final int xoff = 20;
				Row lastRow = null;
				g.setBackground(Color.WHITE);
				g.clearRect(0,0,getWidth(), getHeight());

				int i = 0;
				for (Row row : tb) {
					int y = i * cellSize + yoff;
					if(y > getHeight()) break;
					drawRow(g, cellSize, xoff, y, lastRow, row);
					lastRow = row;
					i++;
				}
				
			}

			private void drawRow(Graphics2D g, int cellSize, int xoff, int yoff, Row previousRow, Row currentRow) {
				for (int j = 0; j < currentRow.cells.size(); j++) {
					int x = j * cellSize + xoff;
					Cell cell = currentRow.cells.get(j);
					if(cell.id.equals(currentRow.changeSet.id)) {
						g.fillOval(x+cellSize/4, yoff +cellSize/4, cellSize/2, cellSize/2);
					} 
					
					if(previousRow != null) {
						for (Cell child : cell.children) {
							final int prev = previousRow.cellIndex(child);
							if(prev != -1) {
								g.drawLine(x+cellSize/2, yoff +cellSize/2, (prev * cellSize + xoff) + cellSize/2, yoff - cellSize + cellSize/2);
							}
						}
					}
				}

				g.drawString(String.format("%s (%s) %s", currentRow.changeSet.id, currentRow.changeSet.user, currentRow.changeSet.summary), currentRow.cells.size() * cellSize + xoff + cellSize, yoff + cellSize/2 + g.getFont().getSize()/2);
			}
		};
		f.setSize(640, 800);
		f.setVisible(true);
		f.repaint();
	}
}
