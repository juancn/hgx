package codng.hgx;

import codng.util.DefaultFunction;
import codng.util.Sequence;

import java.util.ArrayList;
import java.util.Formatter;
import java.util.IdentityHashMap;
import java.util.List;

import static codng.util.Sequences.asSequence;

public class Row {
	public final ChangeSet changeSet;
	public final List<Cell> cells;

	private Row(ChangeSet changeSet) {
		this(changeSet, new ArrayList<Cell>());
		cells.add(new Cell(changeSet.id, null));
	}
	private Row(ChangeSet changeSet, List<Cell> cells) {
		this.changeSet = changeSet;
		this.cells = cells;
	}

	public static Sequence<Row> fromChangeSets(final Iterable<ChangeSet> changesets) {
		return asSequence(changesets).map(new DefaultFunction<ChangeSet, Row>() {
			Row last;
			@Override
			public Row apply(final ChangeSet changeSet) {
				if(last == null) {
					last = new Row(changeSet);
				} else {
					last = last.next(changeSet);
				}
				return last;
			}
		});
	}
	
	private Row next(ChangeSet changeSet) {
		final List<Cell> result = new ArrayList<>();
		Cell branch = null;
		for (Cell cell : cells) {
			if(cell.id.equals(this.changeSet.id)) {
				cell.branch = this.changeSet.branch;
				for (Id parent : this.changeSet.parents()) {
					branch = updateBranch(changeSet, result, branch, cell, parent);
				}
			} else {
				branch = updateBranch(changeSet, result, branch, cell, cell.id);
			}
		}

		if(branch == null) {
			result.add(new Cell(changeSet.id, null));
		}

		return new Row(changeSet, result);
	}

	private Cell updateBranch(ChangeSet changeSet, List<Cell> result, Cell branch, Cell cell, Id id) {
		if (id.equals(changeSet.id) && branch == null) {
			branch = new Cell(id, cell);
			result.add(branch);
		} else if(id.equals(changeSet.id) && branch != null) {
			branch.addChild(cell);
		} else {
			result.add(new Cell(id, cell));
		}
		return branch;
	}

	@Override
	public String toString() {
		return new Formatter()
				.format("%s (%s) %s\n", changeSet.id, changeSet.user, changeSet.summary)
				.format("%s", cells)
				.toString();
	}
	
	private IdentityHashMap<Cell, Integer> indexes;
	public int cellIndex(Cell child) {
		if( indexes == null ) {
			indexes = new IdentityHashMap<>();
			for (int i = 0; i < cells.size(); i++) {
				indexes.put(cells.get(i), i);
			}
		}
		Integer index = indexes.get(child);
		return index == null?-1:index;
	}
}
