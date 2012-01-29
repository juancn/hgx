package codng.hgx.ui;

abstract class Colorizer {
	protected final RowViewer rowViewer;

	protected Colorizer(RowViewer rowViewer) {
		this.rowViewer = rowViewer;
	}

	public abstract RowViewer.Strip colorizeLine(String line);
	
	public static final Colorizer plain(RowViewer rowViewer) {
		return new Colorizer(rowViewer) {
			@Override
			public RowViewer.Strip colorizeLine(String line) {
				return rowViewer.strip().add(rowViewer.code(line));
			}
		};
	}

	public void reset() {}
}
