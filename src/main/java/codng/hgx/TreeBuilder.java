package codng.hgx;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class TreeBuilder 
		implements Iterable<Row>
{
	private final Iterable<ChangeSet> changeSets;

	public TreeBuilder(Iterable<ChangeSet> changeSets) {
		this.changeSets = changeSets;
	}

	@Override
	public Iterator<Row> iterator() {
		return new Iterator<Row>() {
			final Iterator<ChangeSet> chIt = changeSets.iterator();
			Row lastRow;
			@Override
			public boolean hasNext() {
				return chIt.hasNext();
			}

			@Override
			public Row next() {
				if(lastRow == null) {
					lastRow = new Row(chIt.next());
				} else {
					lastRow = lastRow.next(chIt.next());
				}
				return lastRow; 
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	public static void main(String[] args) throws IOException, ParseException {
//		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log")));
		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log")));
//		for (Row row : tb) {
//			System.out.println(row);
//		}
		
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
					for (int j = 0; j < row.cells.size(); j++) {
						int x = j * cellSize + xoff;
						Cell cell = row.cells.get(j);
						if(cell.id.equals(row.changeSet.id)) {
							g.fillOval(x+cellSize/4, y+cellSize/4, cellSize/2, cellSize/2);
						} 
						
						if(lastRow != null) {
							for (Cell child : cell.children) {
								final int prev = lastRow.cellIndex(child);
								if(prev != -1) {
									g.drawLine(x+cellSize/2, y+cellSize/2, (prev * cellSize + xoff) + cellSize/2, y - cellSize + cellSize/2);
								}
							}
						}
					}
					
					g.drawString(String.format("%s (%s) %s", row.changeSet.id, row.changeSet.user, row.changeSet.summary), row.cells.size() * cellSize + xoff + cellSize, y + cellSize/2 + g.getFont().getSize()/2);
					lastRow = row;
					i++;
				}
				
			}
		};
		f.setSize(640, 800);
		f.setVisible(true);
		f.repaint();
	}
}
