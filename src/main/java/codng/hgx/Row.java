package codng.hgx;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

class Row {
	public final ChangeSet changeSet;
	public final List<Cell> cells;

	Row(ChangeSet changeSet) {
		this(changeSet, new ArrayList<Cell>());
		cells.add(new Cell(changeSet.id, null));
	}
	private Row(ChangeSet changeSet, List<Cell> cells) {
		this.changeSet = changeSet;
		this.cells = cells;
	}
	
	Row next(ChangeSet changeSet) {
		final List<Cell> result = new ArrayList<>();

		Cell branch = null;
		for (Cell cell : cells) {
			if(cell.id.equals(this.changeSet.id)) {
				for (Id parent : this.changeSet.parents) {
					Id id = parent;
					if (id.equals(changeSet.id) && branch == null) {
						branch = new Cell(id, cell);
						result.add(branch);
					} else if(id.equals(changeSet.id) && branch != null) {
						branch.addChild(cell);
					} else {
						result.add(new Cell(id, cell));
					}
				}
			} else {
				Id id = cell.id;
				if (id.equals(changeSet.id) && branch == null) {
					branch = new Cell(id, cell);
					result.add(branch);
				} else if(id.equals(changeSet.id) && branch != null) {
					branch.addChild(cell);
				} else {
					result.add(new Cell(id, cell));
				}
						
			}
		}
		
		if(branch == null) {
			result.add(new Cell(changeSet.id, null));
		}

		

		return new Row(changeSet, result);
	}

	@Override
	public String toString() {
		return new Formatter()
				.format("%s (%s) %s\n", changeSet.id, changeSet.user, changeSet.summary)
				.format("%s", cells)
				.toString();
	}

	
	public int cellIndex(Cell child) {
		for (int i = 0; i < cells.size(); i++) {
			Cell cell = cells.get(i);
			if(cell == child) return i;
		}
		return -1;
	}
}
