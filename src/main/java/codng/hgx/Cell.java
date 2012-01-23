package codng.hgx;

import java.util.ArrayList;
import java.util.List;

public class Cell {
	public final Id id;
	public final List<Cell> children = new ArrayList<>();

	Cell(Id id, Cell child) {
		this.id = id;
		if (child != null) {
			addChild(child);
		}
	}

	@Override
	public int hashCode() {
		return id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Cell) {
			Cell cell = (Cell) obj;
			return cell.id.equals(id);
		}
		return false;
	}

	@Override
	public String toString() {
		return "C[" + id + "]";
	}

	public void addChild(Cell cell) {
		children.add(cell);
	}
}

