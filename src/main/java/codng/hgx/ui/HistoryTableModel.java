package codng.hgx.ui;

import codng.hgx.Id;
import codng.hgx.Row;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class HistoryTableModel implements TableModel {
	private final Iterator<Row> historyIt;
	private final List<Row> history = new ArrayList<>();

	private static final String[] columnNames = { "Subject", "Author", "Date", "Changeset"};
	private static final Class[] columnClasses = { Row.class, String.class, Date.class, Id.class};

	public HistoryTableModel(Iterator<Row> historyIt) {
		this.historyIt = historyIt;
		//Load the first 100 rows
		loadUpTo(Integer.getInteger("preload", 100));
	}

	private Row getRow(int index) {
		loadUpTo(index);
		return history.get(index);
	}

	private void loadUpTo(int index) {
		final int previous = history.size();
		while (index >= history.size() && historyIt.hasNext()) {
			history.add(historyIt.next());
		}

		if(index >= previous) {
			fireSizeChanged(previous+1, index);
		}
	}

	@Override
	public int getRowCount() {
		if(historyIt.hasNext()) {
			return history.size() + 1;
		} else {
			return history.size();
		}
	}

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public Class<?> getColumnClass(int columnIndex) {
		return columnClasses[columnIndex];
	}

	@Override
	public boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}

	@Override
	public Object getValueAt(int rowIndex, int columnIndex) {
		final Row row = getRow(rowIndex);
		switch (columnIndex) {
			case 0: return row;
			case 1: return row.changeSet.user;
			case 2: return row.changeSet.date;
			case 3: return row.changeSet.id;
		}
		throw new IndexOutOfBoundsException("Column: " + columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException();
	}

	private final Set<TableModelListener> listeners = new HashSet<>();

	@Override
	public void addTableModelListener(TableModelListener l) {
		listeners.add(l);
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		listeners.remove(l);
	}

	private void fireSizeChanged(int first, int last) {
		for (TableModelListener listener : listeners) {
			listener.tableChanged(new TableModelEvent(this, first, last,
					TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT));
		}
	}
}
