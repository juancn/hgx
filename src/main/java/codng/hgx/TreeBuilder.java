package codng.hgx;

import java.awt.Frame;
import java.awt.Graphics;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

public class TreeBuilder {
	private final List<ChangeSet> changeSets;

	public TreeBuilder(List<ChangeSet> changeSets) {
		this.changeSets = changeSets;
	}

	List<Row> build() {
		List<Row> result = new ArrayList<>();
		Row lastRow = new Row(changeSets.get(0));
		for (ChangeSet changeSet : changeSets.subList(1, changeSets.size())) {
			result.add(lastRow);
			lastRow = lastRow.next(changeSet);
		}
		return result;
	}

	public static void main(String[] args) throws IOException, ParseException {
//		TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history.log")));
		final TreeBuilder tb = new TreeBuilder(ChangeSet.loadFrom(new FileInputStream("/Users/juancn/history-case16146.log")));
		final List<Row> rows = tb.build();
		for (Row row : rows) {
			System.out.println(row);
		}
		
		Frame f = new Frame("test") {
			@Override
			public void update(Graphics g) {
				final int cellSize = 10;
				final int yoff = 20;
				final int xoff = 40;
				Row lastRow = null;
				for (int i = 0; i < rows.size(); i++) {
					Row row = rows.get(i);
					int y = i * cellSize + yoff;
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
					lastRow = row;
				}
			}
		};
		f.setSize(640, 800);
		f.setVisible(true);
		f.repaint();
	}
}
