package codng.hgx.ui;

import codng.hgx.Row;

import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class HistoryTableModel implements TableModel {
	private final Iterator<Row> historyIt;
	private final List<Row> history = new ArrayList<>();

	private static final String[] columnNames = { "Subject", "Author", "Date"}; 
	private static final Class[] columnClasses = { Row.class, String.class, Date.class };
	private int rowCount;

	public HistoryTableModel(Iterator<Row> historyIt) {
		this.historyIt = historyIt;
		//Load the first 100 rows
		getRow(100);
	}

	private Row getRow(int index) {
		while (index >= history.size() && historyIt.hasNext()) {
			history.add(historyIt.next());
		}
		
		return history.get(index);
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
		}
		throw new IndexOutOfBoundsException("Column: " + columnIndex);
	}

	@Override
	public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addTableModelListener(TableModelListener l) {
		// Ignore for now
	}

	@Override
	public void removeTableModelListener(TableModelListener l) {
		
	}
}
